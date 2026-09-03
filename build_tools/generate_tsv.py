import os, zipfile

tiny_jar = r'C:\Users\sense\AppData\Roaming\.minecraft\libraries\net\fabricmc\intermediary\1.21.11\intermediary-1.21.11.jar'
notch_to_inter_class = {}
with zipfile.ZipFile(tiny_jar, 'r') as z:
    for line in z.read('mappings/mappings.tiny').decode('utf-8').splitlines():
        parts = line.split('\t')
        if len(parts) > 1 and parts[0] == 'CLASS':
            notch_to_inter_class[parts[1]] = parts[2]

client_txt = 'mod_build_work/mappings_1_21_11/client_mappings.txt'
mojang_to_inter_class = {}
with open(client_txt, 'r', encoding='utf-8') as f:
    for line in f:
        line_s = line.strip()
        if ' -> ' in line_s and line_s.endswith(':'):
            p = line_s[:-1].split(' -> ')
            m_cls = p[0].replace('.', '/')
            n_cls = p[1]
            if n_cls in notch_to_inter_class:
                mojang_to_inter_class[m_cls] = notch_to_inter_class[n_cls]

lines = []
for m_cls, i_cls in mojang_to_inter_class.items():
    lines.append(f'CLASS\t{m_cls}\t{i_cls}')

# Add fields
fields_map = [
    ('net/minecraft/ChatFormatting', 'DARK_GRAY', 'field_1063'),
    ('net/minecraft/ChatFormatting', 'GOLD', 'field_1065'),
    ('net/minecraft/ChatFormatting', 'GREEN', 'field_1060'),
    ('net/minecraft/ChatFormatting', 'RED', 'field_1061'),
    ('net/minecraft/client/Minecraft', 'gameMode', 'field_1761'),
    ('net/minecraft/client/Minecraft', 'level', 'field_1687'),
    ('net/minecraft/client/Minecraft', 'player', 'field_1724'),
    ('net/minecraft/client/player/LocalPlayer', 'containerMenu', 'field_7512'),
    ('net/minecraft/client/player/LocalPlayer', 'fishing', 'field_7513'),
    ('net/minecraft/client/player/LocalPlayer', 'tickCount', 'field_6012'),
    ('net/minecraft/world/InteractionHand', 'MAIN_HAND', 'field_5808'),
    ('net/minecraft/world/inventory/AbstractContainerMenu', 'containerId', 'field_7763'),
    ('net/minecraft/world/inventory/AbstractContainerMenu', 'slots', 'field_7761'),
    ('net/minecraft/world/inventory/Slot', 'index', 'field_7874'),
    ('net/minecraft/world/item/Items', 'FISHING_ROD', 'field_8378'),
    ('net/minecraft/client/Minecraft', 'options', 'field_1690'),
    ('net/minecraft/client/Options', 'keyUse', 'field_1904'),
    ('net/minecraft/client/Options', 'keyUp', 'field_1894'),
    ('net/minecraft/client/Options', 'keyDown', 'field_1881'),
    ('net/minecraft/client/player/LocalPlayer', 'connection', 'field_3944'),
    ('net/minecraft/world/entity/player/Player', 'inventoryMenu', 'field_7498'),
    ('net/minecraft/client/player/LocalPlayer', 'inventoryMenu', 'field_7498'),
    ('net/minecraft/world/entity/player/Inventory', 'selected', 'field_7545'),
    ('com/mojang/blaze3d/platform/InputConstants$Type', 'KEYSYM', 'field_1668')
]
for o, n, i in fields_map:
    lines.append(f'FIELD\t{o}\t{n}\t{i}')

# Add methods
methods_map = [
    ('net/minecraft/client/KeyMapping', 'consumeClick', '()Z', 'method_1436'),
    ('net/minecraft/client/KeyMapping$Category', 'register', '(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/KeyMapping$Category;', 'method_74698'),
    ('net/minecraft/client/Minecraft', 'getInstance', '()Lnet/minecraft/client/Minecraft;', 'method_1551'),
    ('net/minecraft/client/Minecraft', 'setScreenAndShow', '(Lnet/minecraft/client/gui/screens/Screen;)V', 'method_1507'),
    ('net/minecraft/client/Minecraft', 'setScreen', '(Lnet/minecraft/client/gui/screens/Screen;)V', 'method_1507'),
    ('net/minecraft/client/gui/components/Button', 'builder', '(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;', 'method_46430'),
    ('net/minecraft/client/gui/components/Button$Builder', 'bounds', '(IIII)Lnet/minecraft/client/gui/components/Button$Builder;', 'method_46434'),
    ('net/minecraft/client/gui/components/Button$Builder', 'build', '()Lnet/minecraft/client/gui/components/Button;', 'method_46431'),
    ('net/minecraft/client/gui/components/CycleButton', 'onOffBuilder', '(Z)Lnet/minecraft/client/gui/components/CycleButton$Builder;', 'method_32614'),
    ('net/minecraft/client/gui/components/CycleButton', 'setTooltip', '(Lnet/minecraft/client/gui/components/Tooltip;)V', 'method_47400'),
    ('net/minecraft/client/gui/components/AbstractWidget', 'setTooltip', '(Lnet/minecraft/client/gui/components/Tooltip;)V', 'method_47400'),
    ('net/minecraft/client/gui/components/CycleButton$Builder', 'create', '(IIIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/CycleButton$OnValueChange;)Lnet/minecraft/client/gui/components/CycleButton;', 'method_32617'),
    ('net/minecraft/client/gui/components/Tooltip', 'create', '(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/components/Tooltip;', 'method_47407'),
    ('net/minecraft/client/gui/screens/Screen', 'init', '()V', 'method_25426'),
    ('net/minecraft/client/gui/screens/Screen', 'onClose', '()V', 'method_25419'),
    ('net/minecraft/client/gui/screens/Screen', 'addRenderableWidget', '(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;', 'method_37063'),
    ('net/minecraft/client/gui/screens/Screen', 'addRenderableOnly', '(Lnet/minecraft/client/gui/components/Renderable;)Lnet/minecraft/client/gui/components/Renderable;', 'method_37060'),
    ('ru/euphoria/config/ConfigScreen', 'init', '()V', 'method_25426'),
    ('ru/euphoria/config/ConfigScreen', 'onClose', '()V', 'method_25419'),
    ('ru/euphoria/config/ConfigScreen', 'addRenderableWidget', '(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;', 'method_37063'),
    ('ru/euphoria/config/ConfigScreen', 'addRenderableOnly', '(Lnet/minecraft/client/gui/components/Renderable;)Lnet/minecraft/client/gui/components/Renderable;', 'method_37060'),
    ('net/minecraft/client/multiplayer/ClientLevel', 'isDarkOutside', '()Z', 'method_23886'),
    ('net/minecraft/client/multiplayer/MultiPlayerGameMode', 'useItem', '(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;', 'method_2919'),
    ('net/minecraft/client/player/LocalPlayer', 'getId', '()I', 'method_5628'),
    ('net/minecraft/client/player/LocalPlayer', 'getInventory', '()Lnet/minecraft/world/entity/player/Inventory;', 'method_31548'),
    ('net/minecraft/client/player/LocalPlayer', 'getMainHandItem', '()Lnet/minecraft/world/item/ItemStack;', 'method_6047'),
    ('net/minecraft/client/player/LocalPlayer', 'getXRot', '()F', 'method_36455'),
    ('net/minecraft/client/player/LocalPlayer', 'getYRot', '()F', 'method_36454'),
    ('net/minecraft/client/player/LocalPlayer', 'position', '()Lnet/minecraft/world/phys/Vec3;', 'method_73189'),
    ('net/minecraft/client/player/LocalPlayer', 'displayClientMessage', '(Lnet/minecraft/network/chat/Component;Z)V', 'method_7353'),
    ('net/minecraft/client/player/LocalPlayer', 'setXRot', '(F)V', 'method_36457'),
    ('net/minecraft/client/player/LocalPlayer', 'setYRot', '(F)V', 'method_36456'),
    ('net/minecraft/client/player/LocalPlayer', 'swing', '(Lnet/minecraft/world/InteractionHand;)V', 'method_6104'),
    ('net/minecraft/client/resources/sounds/SoundInstance', 'getIdentifier', '()Lnet/minecraft/resources/Identifier;', 'method_4775'),
    ('net/minecraft/client/resources/sounds/SoundInstance', 'getX', '()D', 'method_4784'),
    ('net/minecraft/client/resources/sounds/SoundInstance', 'getY', '()D', 'method_4779'),
    ('net/minecraft/client/resources/sounds/SoundInstance', 'getZ', '()D', 'method_4778'),
    ('net/minecraft/client/resources/sounds/SoundInstance', 'isRelative', '()Z', 'method_4787'),
    ('net/minecraft/core/Holder', 'value', '()Ljava/lang/Object;', 'comp_349'),
    ('net/minecraft/network/chat/Component', 'empty', '()Lnet/minecraft/network/chat/MutableComponent;', 'method_43473'),
    ('net/minecraft/network/chat/Component', 'literal', '(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;', 'method_43470'),
    ('net/minecraft/network/chat/Component', 'translatable', '(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;', 'method_43471'),
    ('net/minecraft/network/chat/MutableComponent', 'append', '(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;', 'method_27693'),
    ('net/minecraft/network/chat/MutableComponent', 'append', '(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;', 'method_10852'),
    ('net/minecraft/network/chat/MutableComponent', 'withStyle', '(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;', 'method_27692'),
    ('net/minecraft/network/protocol/game/ClientboundSoundEntityPacket', 'getId', '()I', 'method_11883'),
    ('net/minecraft/network/protocol/game/ClientboundSoundEntityPacket', 'getSound', '()Lnet/minecraft/core/Holder;', 'method_11882'),
    ('net/minecraft/network/protocol/game/ClientboundSoundPacket', 'getSound', '()Lnet/minecraft/core/Holder;', 'method_11894'),
    ('net/minecraft/network/protocol/game/ClientboundSoundPacket', 'getX', '()D', 'method_11890'),
    ('net/minecraft/network/protocol/game/ClientboundSoundPacket', 'getY', '()D', 'method_11889'),
    ('net/minecraft/network/protocol/game/ClientboundSoundPacket', 'getZ', '()D', 'method_11893'),
    ('net/minecraft/resources/Identifier', 'fromNamespaceAndPath', '(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/Identifier;', 'method_60655'),
    ('net/minecraft/resources/Identifier', 'getPath', '()Ljava/lang/String;', 'method_12832'),
    ('net/minecraft/sounds/SoundEvent', 'location', '()Lnet/minecraft/resources/Identifier;', 'comp_3319'),
    ('net/minecraft/world/entity/player/Inventory', 'getContainerSize', '()I', 'method_5439'),
    ('net/minecraft/world/entity/player/Inventory', 'getItem', '(I)Lnet/minecraft/world/item/ItemStack;', 'method_5438'),
    ('net/minecraft/world/entity/player/Inventory', 'getSelectedSlot', '()I', 'method_67532'),
    ('net/minecraft/world/entity/projectile/FishingHook', 'getId', '()I', 'method_5628'),
    ('net/minecraft/world/entity/projectile/FishingHook', 'position', '()Lnet/minecraft/world/phys/Vec3;', 'method_73189'),
    ('net/minecraft/world/item/ItemStack', 'getItem', '()Lnet/minecraft/world/item/Item;', 'method_7909'),
    ('net/minecraft/client/KeyMapping', 'setDown', '(Z)V', 'method_23481'),
    ('net/minecraft/client/player/LocalPlayer', 'getX', '()D', 'method_23317'),
    ('net/minecraft/client/player/LocalPlayer', 'getY', '()D', 'method_23318'),
    ('net/minecraft/client/player/LocalPlayer', 'getZ', '()D', 'method_23321'),
    ('net/minecraft/world/entity/Entity', 'getX', '()D', 'method_23317'),
    ('net/minecraft/world/entity/Entity', 'getY', '()D', 'method_23318'),
    ('net/minecraft/world/entity/Entity', 'getZ', '()D', 'method_23321'),
    ('net/minecraft/client/multiplayer/ClientPacketListener', 'sendChat', '(Ljava/lang/String;)V', 'method_45729'),
    ('net/minecraft/client/player/LocalPlayer', 'closeContainer', '()V', 'method_7346'),
    ('net/minecraft/world/entity/player/Player', 'closeContainer', '()V', 'method_7346'),
    ('net/minecraft/world/inventory/Slot', 'getItem', '()Lnet/minecraft/world/item/ItemStack;', 'method_7677'),
    ('net/minecraft/world/item/ItemStack', 'isEmpty', '()Z', 'method_7960'),
    ('net/minecraft/world/item/ItemStack', 'getHoverName', '()Lnet/minecraft/network/chat/Component;', 'method_7964'),
    ('net/minecraft/network/chat/Component', 'getString', '()Ljava/lang/String;', 'method_54160'),
    ('net/minecraft/world/entity/player/Inventory', 'setSelectedSlot', '(I)V', 'method_61496'),
    ('net/minecraft/world/phys/Vec3', 'distanceTo', '(Lnet/minecraft/world/phys/Vec3;)D', 'method_1022')
]
for o, n, d, i in methods_map:
    lines.append(f'METHOD\t{o}\t{n}\t{d}\t{i}')

# Mixin methods
lines.append('MIXIN_METHOD\thandleSoundEvent\tmethod_11146')
lines.append('MIXIN_METHOD\thandleSoundEntityEvent\tmethod_11125')
lines.append('MIXIN_METHOD\tplay\tmethod_4873')

out_p = 'mod_build_work/clean_mappings.tsv'
with open(out_p, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines) + '\n')
print(f'Generated {out_p} with {len(lines)} mapping entries')
