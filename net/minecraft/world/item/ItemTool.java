package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemTool extends ItemToolMaterial implements ItemVanishable {
    private final Tag<Block> blocks;
    protected final float speed;
    private final float attackDamageBaseline;
    private final Multimap<AttributeBase, AttributeModifier> defaultModifiers;

    protected ItemTool(float attackDamage, float attackSpeed, ToolMaterial material, Tag<Block> effectiveBlocks, Item.Info settings) {
        super(material, settings);
        this.blocks = effectiveBlocks;
        this.speed = material.getSpeed();
        this.attackDamageBaseline = attackDamage + material.getAttackDamageBonus();
        Builder<AttributeBase, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(GenericAttributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double)this.attackDamageBaseline, AttributeModifier.Operation.ADDITION));
        builder.put(GenericAttributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double)attackSpeed, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockData state) {
        return this.blocks.isTagged(state.getBlock()) ? this.speed : 1.0F;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.damage(2, attacker, (e) -> {
            e.broadcastItemBreak(EnumItemSlot.MAINHAND);
        });
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if (!world.isClientSide && state.getDestroySpeed(world, pos) != 0.0F) {
            stack.damage(1, miner, (e) -> {
                e.broadcastItemBreak(EnumItemSlot.MAINHAND);
            });
        }

        return true;
    }

    @Override
    public Multimap<AttributeBase, AttributeModifier> getDefaultAttributeModifiers(EnumItemSlot slot) {
        return slot == EnumItemSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public float getAttackDamage() {
        return this.attackDamageBaseline;
    }

    @Override
    public boolean canDestroySpecialBlock(IBlockData state) {
        int i = this.getTier().getLevel();
        if (i < 3 && state.is(TagsBlock.NEEDS_DIAMOND_TOOL)) {
            return false;
        } else if (i < 2 && state.is(TagsBlock.NEEDS_IRON_TOOL)) {
            return false;
        } else {
            return i < 1 && state.is(TagsBlock.NEEDS_STONE_TOOL) ? false : state.is(this.blocks);
        }
    }
}
