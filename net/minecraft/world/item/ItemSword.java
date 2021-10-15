package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;

public class ItemSword extends ItemToolMaterial implements ItemVanishable {
    private final float attackDamage;
    private final Multimap<AttributeBase, AttributeModifier> defaultModifiers;

    public ItemSword(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Item.Info settings) {
        super(toolMaterial, settings);
        this.attackDamage = (float)attackDamage + toolMaterial.getAttackDamageBonus();
        Builder<AttributeBase, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(GenericAttributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double)this.attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(GenericAttributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double)attackSpeed, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    public float getDamage() {
        return this.attackDamage;
    }

    @Override
    public boolean canAttackBlock(IBlockData state, World world, BlockPosition pos, EntityHuman miner) {
        return !miner.isCreative();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockData state) {
        if (state.is(Blocks.COBWEB)) {
            return 15.0F;
        } else {
            Material material = state.getMaterial();
            return material != Material.PLANT && material != Material.REPLACEABLE_PLANT && !state.is(TagsBlock.LEAVES) && material != Material.VEGETABLE ? 1.0F : 1.5F;
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.damage(1, attacker, (e) -> {
            e.broadcastItemBreak(EnumItemSlot.MAINHAND);
        });
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if (state.getDestroySpeed(world, pos) != 0.0F) {
            stack.damage(2, miner, (e) -> {
                e.broadcastItemBreak(EnumItemSlot.MAINHAND);
            });
        }

        return true;
    }

    @Override
    public boolean canDestroySpecialBlock(IBlockData state) {
        return state.is(Blocks.COBWEB);
    }

    @Override
    public Multimap<AttributeBase, AttributeModifier> getDefaultAttributeModifiers(EnumItemSlot slot) {
        return slot == EnumItemSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }
}
