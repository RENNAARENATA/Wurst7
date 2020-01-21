/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"auto eat", "AutoFood", "auto food", "AutoFeeder", "auto feeder",
	"AutoFeeding", "auto feeding", "AutoSoup", "auto soup"})
public final class AutoEatHack extends Hack implements UpdateListener
{
	private final CheckboxSetting eatWhileWalking =
		new CheckboxSetting("Eat while walking", false);
	
	private final EnumSetting<FoodPriority> foodPriority =
		new EnumSetting<>("Prefer food with", FoodPriority.values(),
			FoodPriority.HIGH_SATURATION);
	
	private final CheckboxSetting allowRotten =
		new CheckboxSetting("Allow rotten flesh",
			"Rotten flesh applies a harmless 'hunger' effect.\n"
				+ "It is safe to eat and useful as emergency food.",
			true);
	
	private final CheckboxSetting allowPoison =
		new CheckboxSetting("Allow poison",
			"Poisoned food applies damage over time.\n" + "Not recommended.",
			false);
	
	private final CheckboxSetting allowChorus =
		new CheckboxSetting("Allow chorus fruit",
			"Chorus fruit teleports you to a random location.\n"
				+ "Not recommended.",
			true);
	
	private final CheckboxSetting allowStew = new CheckboxSetting(
		"Allow suspicious stew", "Suspicious stew can apply any potion effect\n"
			+ "for a couple of seconds.",
		false);
	
	private int oldSlot = -1;
	
	public AutoEatHack()
	{
		super("AutoEat", "Automatically eats food when necessary.");
		setCategory(Category.ITEMS);
		addSetting(eatWhileWalking);
		addSetting(foodPriority);
		addSetting(allowRotten);
		addSetting(allowPoison);
		addSetting(allowChorus);
		addSetting(allowStew);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		stopIfEating();
	}
	
	@Override
	public void onUpdate()
	{
		if(!shouldEat())
		{
			stopIfEating();
			return;
		}
		
		int bestSlot = getBestSlot();
		if(bestSlot == -1)
		{
			stopIfEating();
			return;
		}
		
		// save old slot
		if(!isEating())
			oldSlot = MC.player.inventory.selectedSlot;
		
		// set slot
		MC.player.inventory.selectedSlot = bestSlot;
		
		// eat food
		MC.options.keyUse.setPressed(true);
		IMC.getInteractionManager().rightClickItem();
	}
	
	private int getBestSlot()
	{
		int bestSlot = -1;
		float bestSaturation = -1;
		
		for(int i = 0; i < 9; i++)
		{
			// filter out non-food items
			Item item = MC.player.inventory.getInvStack(i).getItem();
			if(!item.isFood())
				continue;
			
			// compare to previously found food
			float saturation = item.getFoodComponent().getSaturationModifier();
			if(saturation > bestSaturation)
			{
				bestSaturation = saturation;
				bestSlot = i;
			}
		}
		
		return bestSlot;
	}
	
	private boolean shouldEat()
	{
		if(!MC.player.canConsume(false))
			return false;
		
		if(!eatWhileWalking.isChecked()
			&& (MC.player.forwardSpeed != 0 || MC.player.sidewaysSpeed != 0))
			return false;
		
		if(isClickable(MC.crosshairTarget))
			return false;
		
		return true;
	}
	
	private boolean isClickable(HitResult hitResult)
	{
		if(hitResult == null)
			return false;
		
		if(hitResult instanceof EntityHitResult)
		{
			Entity entity = ((EntityHitResult)MC.crosshairTarget).getEntity();
			return entity instanceof VillagerEntity
				|| entity instanceof TameableEntity;
		}
		
		if(hitResult instanceof BlockHitResult)
		{
			BlockPos pos = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
			if(pos == null)
				return false;
			
			Block block = MC.world.getBlockState(pos).getBlock();
			return block instanceof BlockWithEntity
				|| block instanceof CraftingTableBlock;
		}
		
		return false;
	}
	
	public boolean isEating()
	{
		return oldSlot != -1;
	}
	
	private void stopIfEating()
	{
		if(!isEating())
			return;
		
		MC.options.keyUse.setPressed(false);
		
		MC.player.inventory.selectedSlot = oldSlot;
		oldSlot = -1;
	}
	
	public static enum FoodPriority
	{
		HIGH_HUNGER("High Food Points"),
		HIGH_SATURATION("High Saturation"),
		LOW_HUNGER("Low Food Points"),
		LOW_SATURATION("Low Saturation");
		
		private final String name;
		
		private FoodPriority(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}