package net.minecraft.network;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInAbilities;
import net.minecraft.network.protocol.game.PacketPlayInAdvancements;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
import net.minecraft.network.protocol.game.PacketPlayInAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayInBEdit;
import net.minecraft.network.protocol.game.PacketPlayInBeacon;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayInBlockPlace;
import net.minecraft.network.protocol.game.PacketPlayInBoatMove;
import net.minecraft.network.protocol.game.PacketPlayInChat;
import net.minecraft.network.protocol.game.PacketPlayInClientCommand;
import net.minecraft.network.protocol.game.PacketPlayInCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayInCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyChange;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyLock;
import net.minecraft.network.protocol.game.PacketPlayInEnchantItem;
import net.minecraft.network.protocol.game.PacketPlayInEntityAction;
import net.minecraft.network.protocol.game.PacketPlayInEntityNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInFlying;
import net.minecraft.network.protocol.game.PacketPlayInHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayInItemName;
import net.minecraft.network.protocol.game.PacketPlayInJigsawGenerate;
import net.minecraft.network.protocol.game.PacketPlayInKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayInPickItem;
import net.minecraft.network.protocol.game.PacketPlayInPong;
import net.minecraft.network.protocol.game.PacketPlayInRecipeDisplayed;
import net.minecraft.network.protocol.game.PacketPlayInRecipeSettings;
import net.minecraft.network.protocol.game.PacketPlayInResourcePackStatus;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandBlock;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandMinecart;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayInSetJigsaw;
import net.minecraft.network.protocol.game.PacketPlayInSettings;
import net.minecraft.network.protocol.game.PacketPlayInSpectate;
import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle;
import net.minecraft.network.protocol.game.PacketPlayInStruct;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.network.protocol.game.PacketPlayInTeleportAccept;
import net.minecraft.network.protocol.game.PacketPlayInTileNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInTrSel;
import net.minecraft.network.protocol.game.PacketPlayInUpdateSign;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.network.protocol.game.PacketPlayInUseItem;
import net.minecraft.network.protocol.game.PacketPlayInVehicleMove;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutActionBarText;
import net.minecraft.network.protocol.game.PacketPlayOutAdvancements;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.protocol.game.PacketPlayOutAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayOutBlockAction;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreak;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreakAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutBorder;
import net.minecraft.network.protocol.game.PacketPlayOutBorderCenter;
import net.minecraft.network.protocol.game.PacketPlayOutBorderLerpSize;
import net.minecraft.network.protocol.game.PacketPlayOutBorderSize;
import net.minecraft.network.protocol.game.PacketPlayOutBorderWarningDelay;
import net.minecraft.network.protocol.game.PacketPlayOutBorderWarningDistance;
import net.minecraft.network.protocol.game.PacketPlayOutBoss;
import net.minecraft.network.protocol.game.PacketPlayOutCamera;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutClearTitles;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutCollect;
import net.minecraft.network.protocol.game.PacketPlayOutCombatEnter;
import net.minecraft.network.protocol.game.PacketPlayOutCombatExit;
import net.minecraft.network.protocol.game.PacketPlayOutCombatKill;
import net.minecraft.network.protocol.game.PacketPlayOutCommands;
import net.minecraft.network.protocol.game.PacketPlayOutCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayOutCustomSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntitySound;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutExplosion;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.game.PacketPlayOutLightUpdate;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.game.PacketPlayOutLookAt;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.network.protocol.game.PacketPlayOutMapChunk;
import net.minecraft.network.protocol.game.PacketPlayOutMount;
import net.minecraft.network.protocol.game.PacketPlayOutMultiBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutOpenBook;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowHorse;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowMerchant;
import net.minecraft.network.protocol.game.PacketPlayOutPing;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.network.protocol.game.PacketPlayOutRecipes;
import net.minecraft.network.protocol.game.PacketPlayOutRemoveEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutResourcePackSend;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutSelectAdvancementTab;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutSetCooldown;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityExperienceOrb;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityPainting;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.network.protocol.game.PacketPlayOutStatistic;
import net.minecraft.network.protocol.game.PacketPlayOutStopSound;
import net.minecraft.network.protocol.game.PacketPlayOutSubtitleText;
import net.minecraft.network.protocol.game.PacketPlayOutTabComplete;
import net.minecraft.network.protocol.game.PacketPlayOutTags;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.network.protocol.game.PacketPlayOutTitleAnimations;
import net.minecraft.network.protocol.game.PacketPlayOutTitleText;
import net.minecraft.network.protocol.game.PacketPlayOutUnloadChunk;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateAttributes;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateHealth;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateTime;
import net.minecraft.network.protocol.game.PacketPlayOutVehicleMove;
import net.minecraft.network.protocol.game.PacketPlayOutVibrationSignal;
import net.minecraft.network.protocol.game.PacketPlayOutViewCentre;
import net.minecraft.network.protocol.game.PacketPlayOutViewDistance;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import net.minecraft.network.protocol.game.PacketPlayOutWindowItems;
import net.minecraft.network.protocol.game.PacketPlayOutWorldEvent;
import net.minecraft.network.protocol.game.PacketPlayOutWorldParticles;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.network.protocol.login.PacketLoginInCustomPayload;
import net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutCustomPayload;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginOutSetCompression;
import net.minecraft.network.protocol.login.PacketLoginOutSuccess;
import net.minecraft.network.protocol.status.PacketStatusInPing;
import net.minecraft.network.protocol.status.PacketStatusInStart;
import net.minecraft.network.protocol.status.PacketStatusOutPong;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import org.apache.logging.log4j.LogManager;

public enum EnumProtocol {
    HANDSHAKING(-1, protocol().addFlow(EnumProtocolDirection.SERVERBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketHandshakingInSetProtocol.class, PacketHandshakingInSetProtocol::new))),
    PLAY(0, protocol().addFlow(EnumProtocolDirection.CLIENTBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketPlayOutSpawnEntity.class, PacketPlayOutSpawnEntity::new).addPacket(PacketPlayOutSpawnEntityExperienceOrb.class, PacketPlayOutSpawnEntityExperienceOrb::new).addPacket(PacketPlayOutSpawnEntityLiving.class, PacketPlayOutSpawnEntityLiving::new).addPacket(PacketPlayOutSpawnEntityPainting.class, PacketPlayOutSpawnEntityPainting::new).addPacket(PacketPlayOutNamedEntitySpawn.class, PacketPlayOutNamedEntitySpawn::new).addPacket(PacketPlayOutVibrationSignal.class, PacketPlayOutVibrationSignal::new).addPacket(PacketPlayOutAnimation.class, PacketPlayOutAnimation::new).addPacket(PacketPlayOutStatistic.class, PacketPlayOutStatistic::new).addPacket(PacketPlayOutBlockBreak.class, PacketPlayOutBlockBreak::new).addPacket(PacketPlayOutBlockBreakAnimation.class, PacketPlayOutBlockBreakAnimation::new).addPacket(PacketPlayOutTileEntityData.class, PacketPlayOutTileEntityData::new).addPacket(PacketPlayOutBlockAction.class, PacketPlayOutBlockAction::new).addPacket(PacketPlayOutBlockChange.class, PacketPlayOutBlockChange::new).addPacket(PacketPlayOutBoss.class, PacketPlayOutBoss::new).addPacket(PacketPlayOutServerDifficulty.class, PacketPlayOutServerDifficulty::new).addPacket(PacketPlayOutChat.class, PacketPlayOutChat::new).addPacket(PacketPlayOutClearTitles.class, PacketPlayOutClearTitles::new).addPacket(PacketPlayOutTabComplete.class, PacketPlayOutTabComplete::new).addPacket(PacketPlayOutCommands.class, PacketPlayOutCommands::new).addPacket(PacketPlayOutCloseWindow.class, PacketPlayOutCloseWindow::new).addPacket(PacketPlayOutWindowItems.class, PacketPlayOutWindowItems::new).addPacket(PacketPlayOutWindowData.class, PacketPlayOutWindowData::new).addPacket(PacketPlayOutSetSlot.class, PacketPlayOutSetSlot::new).addPacket(PacketPlayOutSetCooldown.class, PacketPlayOutSetCooldown::new).addPacket(PacketPlayOutCustomPayload.class, PacketPlayOutCustomPayload::new).addPacket(PacketPlayOutCustomSoundEffect.class, PacketPlayOutCustomSoundEffect::new).addPacket(PacketPlayOutKickDisconnect.class, PacketPlayOutKickDisconnect::new).addPacket(PacketPlayOutEntityStatus.class, PacketPlayOutEntityStatus::new).addPacket(PacketPlayOutExplosion.class, PacketPlayOutExplosion::new).addPacket(PacketPlayOutUnloadChunk.class, PacketPlayOutUnloadChunk::new).addPacket(PacketPlayOutGameStateChange.class, PacketPlayOutGameStateChange::new).addPacket(PacketPlayOutOpenWindowHorse.class, PacketPlayOutOpenWindowHorse::new).addPacket(PacketPlayOutBorder.class, PacketPlayOutBorder::new).addPacket(PacketPlayOutKeepAlive.class, PacketPlayOutKeepAlive::new).addPacket(PacketPlayOutMapChunk.class, PacketPlayOutMapChunk::new).addPacket(PacketPlayOutWorldEvent.class, PacketPlayOutWorldEvent::new).addPacket(PacketPlayOutWorldParticles.class, PacketPlayOutWorldParticles::new).addPacket(PacketPlayOutLightUpdate.class, PacketPlayOutLightUpdate::new).addPacket(PacketPlayOutLogin.class, PacketPlayOutLogin::new).addPacket(PacketPlayOutMap.class, PacketPlayOutMap::new).addPacket(PacketPlayOutOpenWindowMerchant.class, PacketPlayOutOpenWindowMerchant::new).addPacket(PacketPlayOutEntity.PacketPlayOutRelEntityMove.class, PacketPlayOutEntity.PacketPlayOutRelEntityMove::read).addPacket(PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook.class, PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook::read).addPacket(PacketPlayOutEntity.PacketPlayOutEntityLook.class, PacketPlayOutEntity.PacketPlayOutEntityLook::read).addPacket(PacketPlayOutVehicleMove.class, PacketPlayOutVehicleMove::new).addPacket(PacketPlayOutOpenBook.class, PacketPlayOutOpenBook::new).addPacket(PacketPlayOutOpenWindow.class, PacketPlayOutOpenWindow::new).addPacket(PacketPlayOutOpenSignEditor.class, PacketPlayOutOpenSignEditor::new).addPacket(PacketPlayOutPing.class, PacketPlayOutPing::new).addPacket(PacketPlayOutAutoRecipe.class, PacketPlayOutAutoRecipe::new).addPacket(PacketPlayOutAbilities.class, PacketPlayOutAbilities::new).addPacket(PacketPlayOutCombatExit.class, PacketPlayOutCombatExit::new).addPacket(PacketPlayOutCombatEnter.class, PacketPlayOutCombatEnter::new).addPacket(PacketPlayOutCombatKill.class, PacketPlayOutCombatKill::new).addPacket(PacketPlayOutPlayerInfo.class, PacketPlayOutPlayerInfo::new).addPacket(PacketPlayOutLookAt.class, PacketPlayOutLookAt::new).addPacket(PacketPlayOutPosition.class, PacketPlayOutPosition::new).addPacket(PacketPlayOutRecipes.class, PacketPlayOutRecipes::new).addPacket(PacketPlayOutEntityDestroy.class, PacketPlayOutEntityDestroy::new).addPacket(PacketPlayOutRemoveEntityEffect.class, PacketPlayOutRemoveEntityEffect::new).addPacket(PacketPlayOutResourcePackSend.class, PacketPlayOutResourcePackSend::new).addPacket(PacketPlayOutRespawn.class, PacketPlayOutRespawn::new).addPacket(PacketPlayOutEntityHeadRotation.class, PacketPlayOutEntityHeadRotation::new).addPacket(PacketPlayOutMultiBlockChange.class, PacketPlayOutMultiBlockChange::new).addPacket(PacketPlayOutSelectAdvancementTab.class, PacketPlayOutSelectAdvancementTab::new).addPacket(PacketPlayOutActionBarText.class, PacketPlayOutActionBarText::new).addPacket(PacketPlayOutBorderCenter.class, PacketPlayOutBorderCenter::new).addPacket(PacketPlayOutBorderLerpSize.class, PacketPlayOutBorderLerpSize::new).addPacket(PacketPlayOutBorderSize.class, PacketPlayOutBorderSize::new).addPacket(PacketPlayOutBorderWarningDelay.class, PacketPlayOutBorderWarningDelay::new).addPacket(PacketPlayOutBorderWarningDistance.class, PacketPlayOutBorderWarningDistance::new).addPacket(PacketPlayOutCamera.class, PacketPlayOutCamera::new).addPacket(PacketPlayOutHeldItemSlot.class, PacketPlayOutHeldItemSlot::new).addPacket(PacketPlayOutViewCentre.class, PacketPlayOutViewCentre::new).addPacket(PacketPlayOutViewDistance.class, PacketPlayOutViewDistance::new).addPacket(PacketPlayOutSpawnPosition.class, PacketPlayOutSpawnPosition::new).addPacket(PacketPlayOutScoreboardDisplayObjective.class, PacketPlayOutScoreboardDisplayObjective::new).addPacket(PacketPlayOutEntityMetadata.class, PacketPlayOutEntityMetadata::new).addPacket(PacketPlayOutAttachEntity.class, PacketPlayOutAttachEntity::new).addPacket(PacketPlayOutEntityVelocity.class, PacketPlayOutEntityVelocity::new).addPacket(PacketPlayOutEntityEquipment.class, PacketPlayOutEntityEquipment::new).addPacket(PacketPlayOutExperience.class, PacketPlayOutExperience::new).addPacket(PacketPlayOutUpdateHealth.class, PacketPlayOutUpdateHealth::new).addPacket(PacketPlayOutScoreboardObjective.class, PacketPlayOutScoreboardObjective::new).addPacket(PacketPlayOutMount.class, PacketPlayOutMount::new).addPacket(PacketPlayOutScoreboardTeam.class, PacketPlayOutScoreboardTeam::new).addPacket(PacketPlayOutScoreboardScore.class, PacketPlayOutScoreboardScore::new).addPacket(PacketPlayOutSubtitleText.class, PacketPlayOutSubtitleText::new).addPacket(PacketPlayOutUpdateTime.class, PacketPlayOutUpdateTime::new).addPacket(PacketPlayOutTitleText.class, PacketPlayOutTitleText::new).addPacket(PacketPlayOutTitleAnimations.class, PacketPlayOutTitleAnimations::new).addPacket(PacketPlayOutEntitySound.class, PacketPlayOutEntitySound::new).addPacket(PacketPlayOutNamedSoundEffect.class, PacketPlayOutNamedSoundEffect::new).addPacket(PacketPlayOutStopSound.class, PacketPlayOutStopSound::new).addPacket(PacketPlayOutPlayerListHeaderFooter.class, PacketPlayOutPlayerListHeaderFooter::new).addPacket(PacketPlayOutNBTQuery.class, PacketPlayOutNBTQuery::new).addPacket(PacketPlayOutCollect.class, PacketPlayOutCollect::new).addPacket(PacketPlayOutEntityTeleport.class, PacketPlayOutEntityTeleport::new).addPacket(PacketPlayOutAdvancements.class, PacketPlayOutAdvancements::new).addPacket(PacketPlayOutUpdateAttributes.class, PacketPlayOutUpdateAttributes::new).addPacket(PacketPlayOutEntityEffect.class, PacketPlayOutEntityEffect::new).addPacket(PacketPlayOutRecipeUpdate.class, PacketPlayOutRecipeUpdate::new).addPacket(PacketPlayOutTags.class, PacketPlayOutTags::new)).addFlow(EnumProtocolDirection.SERVERBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketPlayInTeleportAccept.class, PacketPlayInTeleportAccept::new).addPacket(PacketPlayInTileNBTQuery.class, PacketPlayInTileNBTQuery::new).addPacket(PacketPlayInDifficultyChange.class, PacketPlayInDifficultyChange::new).addPacket(PacketPlayInChat.class, PacketPlayInChat::new).addPacket(PacketPlayInClientCommand.class, PacketPlayInClientCommand::new).addPacket(PacketPlayInSettings.class, PacketPlayInSettings::new).addPacket(PacketPlayInTabComplete.class, PacketPlayInTabComplete::new).addPacket(PacketPlayInEnchantItem.class, PacketPlayInEnchantItem::new).addPacket(PacketPlayInWindowClick.class, PacketPlayInWindowClick::new).addPacket(PacketPlayInCloseWindow.class, PacketPlayInCloseWindow::new).addPacket(PacketPlayInCustomPayload.class, PacketPlayInCustomPayload::new).addPacket(PacketPlayInBEdit.class, PacketPlayInBEdit::new).addPacket(PacketPlayInEntityNBTQuery.class, PacketPlayInEntityNBTQuery::new).addPacket(PacketPlayInUseEntity.class, PacketPlayInUseEntity::new).addPacket(PacketPlayInJigsawGenerate.class, PacketPlayInJigsawGenerate::new).addPacket(PacketPlayInKeepAlive.class, PacketPlayInKeepAlive::new).addPacket(PacketPlayInDifficultyLock.class, PacketPlayInDifficultyLock::new).addPacket(PacketPlayInFlying.PacketPlayInPosition.class, PacketPlayInFlying.PacketPlayInPosition::read).addPacket(PacketPlayInFlying.PacketPlayInPositionLook.class, PacketPlayInFlying.PacketPlayInPositionLook::read).addPacket(PacketPlayInFlying.PacketPlayInLook.class, PacketPlayInFlying.PacketPlayInLook::read).addPacket(PacketPlayInFlying.StatusOnly.class, PacketPlayInFlying.StatusOnly::read).addPacket(PacketPlayInVehicleMove.class, PacketPlayInVehicleMove::new).addPacket(PacketPlayInBoatMove.class, PacketPlayInBoatMove::new).addPacket(PacketPlayInPickItem.class, PacketPlayInPickItem::new).addPacket(PacketPlayInAutoRecipe.class, PacketPlayInAutoRecipe::new).addPacket(PacketPlayInAbilities.class, PacketPlayInAbilities::new).addPacket(PacketPlayInBlockDig.class, PacketPlayInBlockDig::new).addPacket(PacketPlayInEntityAction.class, PacketPlayInEntityAction::new).addPacket(PacketPlayInSteerVehicle.class, PacketPlayInSteerVehicle::new).addPacket(PacketPlayInPong.class, PacketPlayInPong::new).addPacket(PacketPlayInRecipeSettings.class, PacketPlayInRecipeSettings::new).addPacket(PacketPlayInRecipeDisplayed.class, PacketPlayInRecipeDisplayed::new).addPacket(PacketPlayInItemName.class, PacketPlayInItemName::new).addPacket(PacketPlayInResourcePackStatus.class, PacketPlayInResourcePackStatus::new).addPacket(PacketPlayInAdvancements.class, PacketPlayInAdvancements::new).addPacket(PacketPlayInTrSel.class, PacketPlayInTrSel::new).addPacket(PacketPlayInBeacon.class, PacketPlayInBeacon::new).addPacket(PacketPlayInHeldItemSlot.class, PacketPlayInHeldItemSlot::new).addPacket(PacketPlayInSetCommandBlock.class, PacketPlayInSetCommandBlock::new).addPacket(PacketPlayInSetCommandMinecart.class, PacketPlayInSetCommandMinecart::new).addPacket(PacketPlayInSetCreativeSlot.class, PacketPlayInSetCreativeSlot::new).addPacket(PacketPlayInSetJigsaw.class, PacketPlayInSetJigsaw::new).addPacket(PacketPlayInStruct.class, PacketPlayInStruct::new).addPacket(PacketPlayInUpdateSign.class, PacketPlayInUpdateSign::new).addPacket(PacketPlayInArmAnimation.class, PacketPlayInArmAnimation::new).addPacket(PacketPlayInSpectate.class, PacketPlayInSpectate::new).addPacket(PacketPlayInUseItem.class, PacketPlayInUseItem::new).addPacket(PacketPlayInBlockPlace.class, PacketPlayInBlockPlace::new))),
    STATUS(1, protocol().addFlow(EnumProtocolDirection.SERVERBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketStatusInStart.class, PacketStatusInStart::new).addPacket(PacketStatusInPing.class, PacketStatusInPing::new)).addFlow(EnumProtocolDirection.CLIENTBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketStatusOutServerInfo.class, PacketStatusOutServerInfo::new).addPacket(PacketStatusOutPong.class, PacketStatusOutPong::new))),
    LOGIN(2, protocol().addFlow(EnumProtocolDirection.CLIENTBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketLoginOutDisconnect.class, PacketLoginOutDisconnect::new).addPacket(PacketLoginOutEncryptionBegin.class, PacketLoginOutEncryptionBegin::new).addPacket(PacketLoginOutSuccess.class, PacketLoginOutSuccess::new).addPacket(PacketLoginOutSetCompression.class, PacketLoginOutSetCompression::new).addPacket(PacketLoginOutCustomPayload.class, PacketLoginOutCustomPayload::new)).addFlow(EnumProtocolDirection.SERVERBOUND, (new EnumProtocol.PacketSet()).addPacket(PacketLoginInStart.class, PacketLoginInStart::new).addPacket(PacketLoginInEncryptionBegin.class, PacketLoginInEncryptionBegin::new).addPacket(PacketLoginInCustomPayload.class, PacketLoginInCustomPayload::new)));

    private static final int MIN_PROTOCOL_ID = -1;
    private static final int MAX_PROTOCOL_ID = 2;
    private static final EnumProtocol[] LOOKUP = new EnumProtocol[4];
    private static final Map<Class<? extends Packet<?>>, EnumProtocol> PROTOCOL_BY_PACKET = Maps.newHashMap();
    private final int id;
    private final Map<EnumProtocolDirection, ? extends EnumProtocol.PacketSet<?>> flows;

    private static EnumProtocol.ProtocolBuilder protocol() {
        return new EnumProtocol.ProtocolBuilder();
    }

    private EnumProtocol(int id, EnumProtocol.ProtocolBuilder protocolBuilder) {
        this.id = id;
        this.flows = protocolBuilder.flows;
    }

    @Nullable
    public Integer getPacketId(EnumProtocolDirection side, Packet<?> packet) {
        return this.flows.get(side).getId(packet.getClass());
    }

    @Nullable
    public Packet<?> createPacket(EnumProtocolDirection side, int packetId, PacketDataSerializer buf) {
        return this.flows.get(side).createPacket(packetId, buf);
    }

    public int getId() {
        return this.id;
    }

    @Nullable
    public static EnumProtocol getById(int id) {
        return id >= -1 && id <= 2 ? LOOKUP[id - -1] : null;
    }

    public static EnumProtocol getProtocolForPacket(Packet<?> handler) {
        return PROTOCOL_BY_PACKET.get(handler.getClass());
    }

    static {
        for(EnumProtocol connectionProtocol : values()) {
            int i = connectionProtocol.getId();
            if (i < -1 || i > 2) {
                throw new Error("Invalid protocol ID " + i);
            }

            LOOKUP[i - -1] = connectionProtocol;
            connectionProtocol.flows.forEach((packetFlow, packetSet) -> {
                packetSet.getAllPackets().forEach((class_) -> {
                    if (PROTOCOL_BY_PACKET.containsKey(class_) && PROTOCOL_BY_PACKET.get(class_) != connectionProtocol) {
                        throw new IllegalStateException("Packet " + class_ + " is already assigned to protocol " + PROTOCOL_BY_PACKET.get(class_) + " - can't reassign to " + connectionProtocol);
                    } else {
                        PROTOCOL_BY_PACKET.put(class_, connectionProtocol);
                    }
                });
            });
        }

    }

    static class PacketSet<T extends PacketListener> {
        private final Object2IntMap<Class<? extends Packet<T>>> classToId = SystemUtils.make(new Object2IntOpenHashMap<>(), (object2IntOpenHashMap) -> {
            object2IntOpenHashMap.defaultReturnValue(-1);
        });
        private final List<Function<PacketDataSerializer, ? extends Packet<T>>> idToDeserializer = Lists.newArrayList();

        public <P extends Packet<T>> EnumProtocol.PacketSet<T> addPacket(Class<P> type, Function<PacketDataSerializer, P> function) {
            int i = this.idToDeserializer.size();
            int j = this.classToId.put(type, i);
            if (j != -1) {
                String string = "Packet " + type + " is already registered to ID " + j;
                LogManager.getLogger().fatal(string);
                throw new IllegalArgumentException(string);
            } else {
                this.idToDeserializer.add(function);
                return this;
            }
        }

        @Nullable
        public Integer getId(Class<?> packet) {
            int i = this.classToId.getInt(packet);
            return i == -1 ? null : i;
        }

        @Nullable
        public Packet<?> createPacket(int id, PacketDataSerializer buf) {
            Function<PacketDataSerializer, ? extends Packet<T>> function = this.idToDeserializer.get(id);
            return function != null ? function.apply(buf) : null;
        }

        public Iterable<Class<? extends Packet<?>>> getAllPackets() {
            return Iterables.unmodifiableIterable(this.classToId.keySet());
        }
    }

    static class ProtocolBuilder {
        final Map<EnumProtocolDirection, EnumProtocol.PacketSet<?>> flows = Maps.newEnumMap(EnumProtocolDirection.class);

        public <T extends PacketListener> EnumProtocol.ProtocolBuilder addFlow(EnumProtocolDirection side, EnumProtocol.PacketSet<T> handler) {
            this.flows.put(side, handler);
            return this;
        }
    }
}
