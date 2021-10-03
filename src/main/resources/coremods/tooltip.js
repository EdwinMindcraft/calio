var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var Opcodes = Java.type('org.objectweb.asm.Opcodes')
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode')
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode')
var InsnList = Java.type('org.objectweb.asm.tree.InsnList')

function initializeCoreMod() {
    return {
        "calio_remove_italic": {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraftforge.common.extensions.IForgeItemStack',
                'methodName': 'getHighlightTip',
                'methodDesc': '(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;'
            },
            'transformer': function (node) {
                var instructions = new InsnList();
                //param1 = CoreHelper.removeItalic(this.self(), param1);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, 'net/minecraftforge/common/extensions/IForgeItemStack', 'self', '()Lnet/minecraft/world/item/ItemStack;', true));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'io/github/edwinmindcraft/calio/common/util/CoreHelper', 'removeItalic', '(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;'));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));
                node.instructions.insert(instructions);
                return node;
            }
        },
        "calio_remove_flag": {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.world.item.ItemStack',
                'methodName': 'resetHoverName',
                'methodDesc': '()V'
            },
            'transformer': function (node) {
                var instructions = new InsnList();
                //param1 = CoreHelper.removeItalic(this.self(), param1);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'io/github/edwinmindcraft/calio/common/util/CoreHelper', 'removeNonItalicFlag', '(Lnet/minecraft/world/item/ItemStack;)V'));
                node.instructions.insert(instructions);
                return node;
            }
        }
    }
}
