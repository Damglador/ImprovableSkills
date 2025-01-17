package org.zeith.improvableskills.utils.loot;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.zeith.hammerlib.util.java.Cast;
import org.zeith.improvableskills.api.registry.PlayerSkillBase;
import org.zeith.improvableskills.data.PlayerDataManager;

import java.util.Optional;

public class LootConditionSkillScroll
		extends LootItemRandomChanceCondition
{
	private final PlayerSkillBase skill;
	
	public LootConditionSkillScroll(float probability, PlayerSkillBase skill)
	{
		super(probability);
		this.skill = skill;
	}
	
	public Component getContextComponent()
	{
		return skill.getLocalizedName();
	}
	
	@Override
	public LootItemConditionType getType()
	{
		return LootItemConditions.RANDOM_CHANCE;
	}
	
	private Player findPlayer(LootContext context, LootContextParam<Entity> par)
	{
		if(!context.hasParam(par)) return null;
		var ent = context.getParamOrNull(par);
		if(!(ent instanceof Player) && ent instanceof LivingEntity le)
		{
			var brain = le.getBrain();
			Optional<Player> optional = optMemory(brain, MemoryModuleType.NEAREST_VISIBLE_PLAYER)
					.or(() -> optMemory(brain, MemoryModuleType.INTERACTION_TARGET).map(Cast.convertTo(Player.class)));
			if(optional.isPresent()) return optional.orElse(null);
		}
		return ent instanceof Player pl && !(pl instanceof FakePlayer) ? pl : null;
	}
	
	protected <T> Optional<T> optMemory(Brain<?> brain, MemoryModuleType<T> type)
	{
		if(!brain.hasMemoryValue(type)) return Optional.empty();
		return brain.getMemory(type);
	}
	
	@Override
	public boolean test(LootContext context)
	{
		Player p = findPlayer(context, LootContextParams.KILLER_ENTITY);
		if(p == null) p = findPlayer(context, LootContextParams.THIS_ENTITY);
		if(p == null) p = findPlayer(context, LootContextParams.DIRECT_KILLER_ENTITY);
		
		// Find the closest player if context is missing a player entity.
		if(p == null && !(context.hasParam(LootContextParams.KILLER_ENTITY)
						  || context.hasParam(LootContextParams.THIS_ENTITY)
						  || context.hasParam(LootContextParams.DIRECT_KILLER_ENTITY))
		)
		{
			Vec3 pos = context.getParamOrNull(LootContextParams.ORIGIN);
			if(pos != null) p = context.getLevel().getNearestPlayer(
					TargetingConditions.forNonCombat()
							.ignoreLineOfSight()
							.range(Double.POSITIVE_INFINITY)
							.ignoreInvisibilityTesting(),
					pos.x, pos.y, pos.z
			);
		}
		
		if(p != null)
			return PlayerDataManager.handleDataSafely(p, data -> !data.hasSkillScroll(skill) ||
																 data.getSkillProgress(skill) < 1F, true);
		
		return false;
	}
}