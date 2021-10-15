package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;

public class CriterionConditionEntityEquipment {
    public static final CriterionConditionEntityEquipment ANY = new CriterionConditionEntityEquipment(CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY);
    public static final CriterionConditionEntityEquipment CAPTAIN = new CriterionConditionEntityEquipment(CriterionConditionItem.Builder.item().of(Items.WHITE_BANNER).hasNbt(Raid.getLeaderBannerInstance().getTag()).build(), CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY, CriterionConditionItem.ANY);
    private final CriterionConditionItem head;
    private final CriterionConditionItem chest;
    private final CriterionConditionItem legs;
    private final CriterionConditionItem feet;
    private final CriterionConditionItem mainhand;
    private final CriterionConditionItem offhand;

    public CriterionConditionEntityEquipment(CriterionConditionItem head, CriterionConditionItem chest, CriterionConditionItem legs, CriterionConditionItem feet, CriterionConditionItem mainhand, CriterionConditionItem offhand) {
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
        this.mainhand = mainhand;
        this.offhand = offhand;
    }

    public boolean matches(@Nullable Entity entity) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof EntityLiving)) {
            return false;
        } else {
            EntityLiving livingEntity = (EntityLiving)entity;
            if (!this.head.matches(livingEntity.getEquipment(EnumItemSlot.HEAD))) {
                return false;
            } else if (!this.chest.matches(livingEntity.getEquipment(EnumItemSlot.CHEST))) {
                return false;
            } else if (!this.legs.matches(livingEntity.getEquipment(EnumItemSlot.LEGS))) {
                return false;
            } else if (!this.feet.matches(livingEntity.getEquipment(EnumItemSlot.FEET))) {
                return false;
            } else if (!this.mainhand.matches(livingEntity.getEquipment(EnumItemSlot.MAINHAND))) {
                return false;
            } else {
                return this.offhand.matches(livingEntity.getEquipment(EnumItemSlot.OFFHAND));
            }
        }
    }

    public static CriterionConditionEntityEquipment fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "equipment");
            CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("head"));
            CriterionConditionItem itemPredicate2 = CriterionConditionItem.fromJson(jsonObject.get("chest"));
            CriterionConditionItem itemPredicate3 = CriterionConditionItem.fromJson(jsonObject.get("legs"));
            CriterionConditionItem itemPredicate4 = CriterionConditionItem.fromJson(jsonObject.get("feet"));
            CriterionConditionItem itemPredicate5 = CriterionConditionItem.fromJson(jsonObject.get("mainhand"));
            CriterionConditionItem itemPredicate6 = CriterionConditionItem.fromJson(jsonObject.get("offhand"));
            return new CriterionConditionEntityEquipment(itemPredicate, itemPredicate2, itemPredicate3, itemPredicate4, itemPredicate5, itemPredicate6);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("head", this.head.serializeToJson());
            jsonObject.add("chest", this.chest.serializeToJson());
            jsonObject.add("legs", this.legs.serializeToJson());
            jsonObject.add("feet", this.feet.serializeToJson());
            jsonObject.add("mainhand", this.mainhand.serializeToJson());
            jsonObject.add("offhand", this.offhand.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        private CriterionConditionItem head = CriterionConditionItem.ANY;
        private CriterionConditionItem chest = CriterionConditionItem.ANY;
        private CriterionConditionItem legs = CriterionConditionItem.ANY;
        private CriterionConditionItem feet = CriterionConditionItem.ANY;
        private CriterionConditionItem mainhand = CriterionConditionItem.ANY;
        private CriterionConditionItem offhand = CriterionConditionItem.ANY;

        public static CriterionConditionEntityEquipment.Builder equipment() {
            return new CriterionConditionEntityEquipment.Builder();
        }

        public CriterionConditionEntityEquipment.Builder head(CriterionConditionItem head) {
            this.head = head;
            return this;
        }

        public CriterionConditionEntityEquipment.Builder chest(CriterionConditionItem chest) {
            this.chest = chest;
            return this;
        }

        public CriterionConditionEntityEquipment.Builder legs(CriterionConditionItem legs) {
            this.legs = legs;
            return this;
        }

        public CriterionConditionEntityEquipment.Builder feet(CriterionConditionItem feet) {
            this.feet = feet;
            return this;
        }

        public CriterionConditionEntityEquipment.Builder mainhand(CriterionConditionItem mainhand) {
            this.mainhand = mainhand;
            return this;
        }

        public CriterionConditionEntityEquipment.Builder offhand(CriterionConditionItem offhand) {
            this.offhand = offhand;
            return this;
        }

        public CriterionConditionEntityEquipment build() {
            return new CriterionConditionEntityEquipment(this.head, this.chest, this.legs, this.feet, this.mainhand, this.offhand);
        }
    }
}
