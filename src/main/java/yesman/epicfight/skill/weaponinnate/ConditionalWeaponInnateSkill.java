package yesman.epicfight.skill.weaponinnate;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationProvider.AttackAnimationProvider;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class ConditionalWeaponInnateSkill extends WeaponInnateSkill {
	public static class Builder extends Skill.Builder<ConditionalWeaponInnateSkill> {
		protected Function<ServerPlayerPatch, Integer> selector;
		protected ResourceLocation[] animationLocations;
		
		public Builder setCategory(SkillCategory category) {
			this.category = category;
			return this;
		}
		
		public Builder setActivateType(ActivateType activateType) {
			this.activateType = activateType;
			return this;
		}
		
		public Builder setResource(Resource resource) {
			this.resource = resource;
			return this;
		}
		
		public Builder setSelector(Function<ServerPlayerPatch, Integer> selector) {
			this.selector = selector;
			return this;
		}
		
		public Builder setAnimations(ResourceLocation... animationLocations) {
			this.animationLocations = animationLocations;
			return this;
		}
	}
	
	public static ConditionalWeaponInnateSkill.Builder createConditionalWeaponInnateBuilder() {
		return (new ConditionalWeaponInnateSkill.Builder()).setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.WEAPON_INNATE_ENERGY);
	}
	
	protected final AttackAnimationProvider[] attackAnimations;
	protected final Function<ServerPlayerPatch, Integer> selector;
	
	public ConditionalWeaponInnateSkill(ConditionalWeaponInnateSkill.Builder builder) {
		super(builder);
		this.properties = Lists.newArrayList();
		this.attackAnimations = new AttackAnimationProvider[builder.animationLocations.length];
		this.selector = builder.selector;
		
		for (int i = 0; i < builder.animationLocations.length; i++) {
			final int idx = i;
			this.attackAnimations[idx] = () -> (AttackAnimation)AnimationManager.getInstance().byKey(builder.animationLocations[idx].toString());
		}
	}
	
	@Override
	public List<Component> getTooltipOnItem(ItemStack itemStack, CapabilityItem cap, PlayerPatch<?> playerCap) {
		List<Component> list = super.getTooltipOnItem(itemStack, cap, playerCap);
		this.generateTooltipforPhase(list, itemStack, cap, playerCap, this.properties.get(0), "Each Strikes:");
		
		return list;
	}
	
	@Override
	public WeaponInnateSkill registerPropertiesToAnimation() {
		for (AttackAnimationProvider animationProvider : this.attackAnimations) {
			AttackAnimation anim = animationProvider.get();
			
			for (Phase phase : anim.phases) {
				phase.addProperties(this.properties.get(0).entrySet());
			}
		}
		
		return this;
	}
	
	@Override
	public void executeOnServer(ServerPlayerPatch executer, FriendlyByteBuf args) {
		executer.playAnimationSynchronized(this.attackAnimations[this.getAnimationInCondition(executer)].get(), 0);
		super.executeOnServer(executer, args);
	}
	
	public int getAnimationInCondition(ServerPlayerPatch executer) {
		return selector.apply(executer);
	}
}