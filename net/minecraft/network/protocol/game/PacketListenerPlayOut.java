package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketListener;

public interface PacketListenerPlayOut extends PacketListener {
    void handleAddEntity(PacketPlayOutSpawnEntity packet);

    void handleAddExperienceOrb(PacketPlayOutSpawnEntityExperienceOrb packet);

    void handleAddVibrationSignal(PacketPlayOutVibrationSignal packet);

    void handleAddMob(PacketPlayOutSpawnEntityLiving packet);

    void handleAddObjective(PacketPlayOutScoreboardObjective packet);

    void handleAddPainting(PacketPlayOutSpawnEntityPainting packet);

    void handleAddPlayer(PacketPlayOutNamedEntitySpawn packet);

    void handleAnimate(PacketPlayOutAnimation packet);

    void handleAwardStats(PacketPlayOutStatistic packet);

    void handleAddOrRemoveRecipes(PacketPlayOutRecipes packet);

    void handleBlockDestruction(PacketPlayOutBlockBreakAnimation packet);

    void handleOpenSignEditor(PacketPlayOutOpenSignEditor packet);

    void handleBlockEntityData(PacketPlayOutTileEntityData packet);

    void handleBlockEvent(PacketPlayOutBlockAction packet);

    void handleBlockUpdate(PacketPlayOutBlockChange packet);

    void handleChat(PacketPlayOutChat packet);

    void handleChunkBlocksUpdate(PacketPlayOutMultiBlockChange packet);

    void handleMapItemData(PacketPlayOutMap packet);

    void handleContainerClose(PacketPlayOutCloseWindow packet);

    void handleContainerContent(PacketPlayOutWindowItems packet);

    void handleHorseScreenOpen(PacketPlayOutOpenWindowHorse packet);

    void handleContainerSetData(PacketPlayOutWindowData packet);

    void handleContainerSetSlot(PacketPlayOutSetSlot packet);

    void handleCustomPayload(PacketPlayOutCustomPayload packet);

    void handleDisconnect(PacketPlayOutKickDisconnect packet);

    void handleEntityEvent(PacketPlayOutEntityStatus packet);

    void handleEntityLinkPacket(PacketPlayOutAttachEntity packet);

    void handleSetEntityPassengersPacket(PacketPlayOutMount packet);

    void handleExplosion(PacketPlayOutExplosion packet);

    void handleGameEvent(PacketPlayOutGameStateChange packet);

    void handleKeepAlive(PacketPlayOutKeepAlive packet);

    void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet);

    void handleForgetLevelChunk(PacketPlayOutUnloadChunk packet);

    void handleLevelEvent(PacketPlayOutWorldEvent packet);

    void handleLogin(PacketPlayOutLogin packet);

    void handleMoveEntity(PacketPlayOutEntity packet);

    void handleMovePlayer(PacketPlayOutPosition packet);

    void handleParticleEvent(PacketPlayOutWorldParticles packet);

    void handlePing(PacketPlayOutPing packet);

    void handlePlayerAbilities(PacketPlayOutAbilities packet);

    void handlePlayerInfo(PacketPlayOutPlayerInfo packet);

    void handleRemoveEntities(PacketPlayOutEntityDestroy packet);

    void handleRemoveMobEffect(PacketPlayOutRemoveEntityEffect packet);

    void handleRespawn(PacketPlayOutRespawn packet);

    void handleRotateMob(PacketPlayOutEntityHeadRotation packet);

    void handleSetCarriedItem(PacketPlayOutHeldItemSlot packet);

    void handleSetDisplayObjective(PacketPlayOutScoreboardDisplayObjective packet);

    void handleSetEntityData(PacketPlayOutEntityMetadata packet);

    void handleSetEntityMotion(PacketPlayOutEntityVelocity packet);

    void handleSetEquipment(PacketPlayOutEntityEquipment packet);

    void handleSetExperience(PacketPlayOutExperience packet);

    void handleSetHealth(PacketPlayOutUpdateHealth packet);

    void handleSetPlayerTeamPacket(PacketPlayOutScoreboardTeam packet);

    void handleSetScore(PacketPlayOutScoreboardScore packet);

    void handleSetSpawn(PacketPlayOutSpawnPosition packet);

    void handleSetTime(PacketPlayOutUpdateTime packet);

    void handleSoundEvent(PacketPlayOutNamedSoundEffect packet);

    void handleSoundEntityEvent(PacketPlayOutEntitySound packet);

    void handleCustomSoundEvent(PacketPlayOutCustomSoundEffect packet);

    void handleTakeItemEntity(PacketPlayOutCollect packet);

    void handleTeleportEntity(PacketPlayOutEntityTeleport packet);

    void handleUpdateAttributes(PacketPlayOutUpdateAttributes packet);

    void handleUpdateMobEffect(PacketPlayOutEntityEffect packet);

    void handleUpdateTags(PacketPlayOutTags packet);

    void handlePlayerCombatEnd(PacketPlayOutCombatExit packet);

    void handlePlayerCombatEnter(PacketPlayOutCombatEnter packet);

    void handlePlayerCombatKill(PacketPlayOutCombatKill packet);

    void handleChangeDifficulty(PacketPlayOutServerDifficulty packet);

    void handleSetCamera(PacketPlayOutCamera packet);

    void handleInitializeBorder(PacketPlayOutBorder packet);

    void handleSetBorderLerpSize(PacketPlayOutBorderLerpSize packet);

    void handleSetBorderSize(PacketPlayOutBorderSize packet);

    void handleSetBorderWarningDelay(PacketPlayOutBorderWarningDelay packet);

    void handleSetBorderWarningDistance(PacketPlayOutBorderWarningDistance packet);

    void handleSetBorderCenter(PacketPlayOutBorderCenter packet);

    void handleTabListCustomisation(PacketPlayOutPlayerListHeaderFooter packet);

    void handleResourcePack(PacketPlayOutResourcePackSend packet);

    void handleBossUpdate(PacketPlayOutBoss packet);

    void handleItemCooldown(PacketPlayOutSetCooldown packet);

    void handleMoveVehicle(PacketPlayOutVehicleMove packet);

    void handleUpdateAdvancementsPacket(PacketPlayOutAdvancements packet);

    void handleSelectAdvancementsTab(PacketPlayOutSelectAdvancementTab packet);

    void handlePlaceRecipe(PacketPlayOutAutoRecipe packet);

    void handleCommands(PacketPlayOutCommands packet);

    void handleStopSoundEvent(PacketPlayOutStopSound packet);

    void handleCommandSuggestions(PacketPlayOutTabComplete packet);

    void handleUpdateRecipes(PacketPlayOutRecipeUpdate packet);

    void handleLookAt(PacketPlayOutLookAt packet);

    void handleTagQueryPacket(PacketPlayOutNBTQuery packet);

    void handleLightUpdatePacket(PacketPlayOutLightUpdate packet);

    void handleOpenBook(PacketPlayOutOpenBook packet);

    void handleOpenScreen(PacketPlayOutOpenWindow packet);

    void handleMerchantOffers(PacketPlayOutOpenWindowMerchant packet);

    void handleSetChunkCacheRadius(PacketPlayOutViewDistance packet);

    void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet);

    void handleSetChunkCacheCenter(PacketPlayOutViewCentre packet);

    void handleBlockBreakAck(PacketPlayOutBlockBreak packet);

    void setActionBarText(PacketPlayOutActionBarText packet);

    void setSubtitleText(PacketPlayOutSubtitleText packet);

    void setTitleText(PacketPlayOutTitleText packet);

    void setTitlesAnimation(PacketPlayOutTitleAnimations packet);

    void handleTitlesClear(PacketPlayOutClearTitles packet);
}
