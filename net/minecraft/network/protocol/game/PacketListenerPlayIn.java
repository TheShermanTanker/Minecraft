package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketListener;

public interface PacketListenerPlayIn extends PacketListener {
    void handleAnimate(PacketPlayInArmAnimation packet);

    void handleChat(PacketPlayInChat packet);

    void handleClientCommand(PacketPlayInClientCommand packet);

    void handleClientInformation(PacketPlayInSettings packet);

    void handleContainerButtonClick(PacketPlayInEnchantItem packet);

    void handleContainerClick(PacketPlayInWindowClick packet);

    void handlePlaceRecipe(PacketPlayInAutoRecipe packet);

    void handleContainerClose(PacketPlayInCloseWindow packet);

    void handleCustomPayload(PacketPlayInCustomPayload packet);

    void handleInteract(PacketPlayInUseEntity packet);

    void handleKeepAlive(PacketPlayInKeepAlive packet);

    void handleMovePlayer(PacketPlayInFlying packet);

    void handlePong(PacketPlayInPong packet);

    void handlePlayerAbilities(PacketPlayInAbilities packet);

    void handlePlayerAction(PacketPlayInBlockDig packet);

    void handlePlayerCommand(PacketPlayInEntityAction packet);

    void handlePlayerInput(PacketPlayInSteerVehicle packet);

    void handleSetCarriedItem(PacketPlayInHeldItemSlot packet);

    void handleSetCreativeModeSlot(PacketPlayInSetCreativeSlot packet);

    void handleSignUpdate(PacketPlayInUpdateSign packet);

    void handleUseItemOn(PacketPlayInUseItem packet);

    void handleUseItem(PacketPlayInBlockPlace packet);

    void handleTeleportToEntityPacket(PacketPlayInSpectate packet);

    void handleResourcePackResponse(PacketPlayInResourcePackStatus packet);

    void handlePaddleBoat(PacketPlayInBoatMove packet);

    void handleMoveVehicle(PacketPlayInVehicleMove packet);

    void handleAcceptTeleportPacket(PacketPlayInTeleportAccept packet);

    void handleRecipeBookSeenRecipePacket(PacketPlayInRecipeDisplayed packet);

    void handleRecipeBookChangeSettingsPacket(PacketPlayInRecipeSettings packet);

    void handleSeenAdvancements(PacketPlayInAdvancements packet);

    void handleCustomCommandSuggestions(PacketPlayInTabComplete packet);

    void handleSetCommandBlock(PacketPlayInSetCommandBlock packet);

    void handleSetCommandMinecart(PacketPlayInSetCommandMinecart packet);

    void handlePickItem(PacketPlayInPickItem packet);

    void handleRenameItem(PacketPlayInItemName packet);

    void handleSetBeaconPacket(PacketPlayInBeacon packet);

    void handleSetStructureBlock(PacketPlayInStruct packet);

    void handleSelectTrade(PacketPlayInTrSel packet);

    void handleEditBook(PacketPlayInBEdit packet);

    void handleEntityTagQuery(PacketPlayInEntityNBTQuery packet);

    void handleBlockEntityTagQuery(PacketPlayInTileNBTQuery packet);

    void handleSetJigsawBlock(PacketPlayInSetJigsaw packet);

    void handleJigsawGenerate(PacketPlayInJigsawGenerate packet);

    void handleChangeDifficulty(PacketPlayInDifficultyChange packet);

    void handleLockDifficulty(PacketPlayInDifficultyLock packet);
}
