package org.pitest.bytecode.analysis;

import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.prelude.Prelude;
import org.pitest.sequence.Context;
import org.pitest.sequence.Match;
import org.pitest.sequence.SlotRead;
import org.pitest.sequence.SlotWrite;

public class InstructionMatchers {
  
  public static Match<AbstractInsnNode> anyInstruction() {
    return Match.always();
  }
  
  public static Match<AbstractInsnNode> opCode(final int opcode) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode a) {
        return a.getOpcode() == opcode;
      }
    };
  }
  
  public static <T extends AbstractInsnNode> Match<AbstractInsnNode> isA(
      final Class<T> cls) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode>  c, AbstractInsnNode a) {
        return a.getClass().isAssignableFrom(cls);
      }
    };
  }

  public static Match<AbstractInsnNode> increments(final SlotRead<Integer> counterVariable) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        if (a instanceof IincInsnNode) {
          IincInsnNode inc = (IincInsnNode) a;
          return context.retrieve(counterVariable).contains(Prelude.isEqualTo(inc.var));
        } else {
          return false;
        }
      }
    };
  }

  public static Match<AbstractInsnNode> stores(
      final SlotWrite<Integer> counterVariable) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        if (!(a instanceof VarInsnNode)) {
          return false;
        }
        VarInsnNode varNode = (VarInsnNode) a;

        if (a.getOpcode() == Opcodes.ISTORE) {
          context.store(counterVariable, varNode.var);
          return true;
        }
        return false;
      }

    };
  }
  
  public static Match<AbstractInsnNode> storesTo(
      final SlotRead<Integer> counterVariable) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        if (!(a instanceof VarInsnNode)) {
          return false;
        }
        VarInsnNode varNode = (VarInsnNode) a;

        return (a.getOpcode() == Opcodes.ISTORE) 
            && context.retrieve(counterVariable).contains(Prelude.isEqualTo(varNode.var));
      }

    };
  }

  public static Match<AbstractInsnNode> load(
      final SlotRead<Integer> counterVariable) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        if (a.getOpcode() != Opcodes.ILOAD) {
          return false;
        }

        VarInsnNode varNode = (VarInsnNode) a;
        return context.retrieve(counterVariable).contains(Prelude.isEqualTo(varNode.var));
      }

    };
  }
  
   
  public static Match<AbstractInsnNode> anIntegerConstant() {
    return opCode(ICONST_0)
        .or(opCode(ICONST_1))
        .or(opCode(ICONST_2))
        .or(opCode(ICONST_3))
        .or(opCode(ICONST_4))
        .or(opCode(ICONST_5));
  }
  
  public static Match<AbstractInsnNode> aLabelNode(SlotWrite<LabelNode> slot) {
    return isA(LabelNode.class).and(writeNodeToSlot(slot, LabelNode.class));
  }
  
  public static Match<AbstractInsnNode> aJump() {
    return isA(JumpInsnNode.class);
  }
  
  
  public static Match<AbstractInsnNode> aConditionalJump() {
    return new  Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        return (t instanceof JumpInsnNode) 
            && t.getOpcode() != Opcodes.GOTO
            && t.getOpcode() != Opcodes.JSR;
      }   
    };
  }
  
  public static <T extends AbstractInsnNode> Match<AbstractInsnNode> writeNodeToSlot(final SlotWrite<T> slot, final Class<T> clazz) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        if (clazz.isAssignableFrom(t.getClass()) ) {
          c.store(slot, clazz.cast(t));
          return true;
        }
        return false;
      }
      
    };
  }
  
  public static  Match<AbstractInsnNode> methodCallThatReturns(final ClassName type) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        if ( t instanceof MethodInsnNode ) {
          return ((MethodInsnNode) t).desc.endsWith(type.asInternalName() + ";");
        }
        return false;
      }
      
    };
  }
  
  public static  Match<AbstractInsnNode> methodCall() {
    return isA(MethodInsnNode.class);
  }
  
  
  public static  Match<AbstractInsnNode> methodCallTo(final ClassName owner, final String name) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        if ( t instanceof MethodInsnNode ) {
          MethodInsnNode call = (MethodInsnNode) t;
          
          return call.name.equals(name) && call.owner.equals(owner.asInternalName());
        }
        return false;
      }
    };
  }
  
  
  private static Match<AbstractInsnNode> storeJumpTarget(
      final SlotWrite<LabelNode> label) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
        if (t instanceof JumpInsnNode ) {
          c.store(label, ((JumpInsnNode) t).label);
          return true;
        }
        return false;
      }
      
    };
  }

  public static Match<AbstractInsnNode> jumpsTo(
      final SlotRead<LabelNode> loopStart) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        if (!(a instanceof JumpInsnNode)) {
          return false;
        }
        JumpInsnNode jump = (JumpInsnNode) a;
        
        return context.retrieve(loopStart).contains(Prelude.isEqualTo(jump.label));
      }
    };
  }
  
  public static Match<AbstractInsnNode> gotoLabel(
      final SlotWrite<LabelNode> loopEnd) {
        return opCode(Opcodes.GOTO).and(storeJumpTarget(loopEnd));
  }
  
  public static Match<AbstractInsnNode> labelNode(
      final SlotRead<LabelNode> loopEnd) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode t) {
       if (!(t instanceof LabelNode)) {
         return false;
       }
       
       LabelNode l = (LabelNode) t;
       return c.retrieve(loopEnd).contains(Prelude.isEqualTo(l));
       
      }
      
    };
  }
  
  public static Match<AbstractInsnNode> debug(final String msg) {
    return new Match<AbstractInsnNode>() {
      @Override
      public boolean test(Context<AbstractInsnNode> context, AbstractInsnNode a) {
        context.debug(msg);
        return true;
      }
    };
  }
}
