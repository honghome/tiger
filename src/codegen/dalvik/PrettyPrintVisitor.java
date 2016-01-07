package codegen.dalvik;

import codegen.dalvik.Ast.Class;
import codegen.dalvik.Ast.Class.ClassSingle;
import codegen.dalvik.Ast.Dec;
import codegen.dalvik.Ast.Dec.DecSingle;
import codegen.dalvik.Ast.MainClass.MainClassSingle;
import codegen.dalvik.Ast.Method;
import codegen.dalvik.Ast.Method.MethodSingle;
import codegen.dalvik.Ast.Program.ProgramSingle;
import codegen.dalvik.Ast.Stm;
import codegen.dalvik.Ast.Type;
import codegen.dalvik.Ast.Type.*;
import codegen.dalvik.Ast.Stm.*;

public class PrettyPrintVisitor implements Visitor {
	private java.io.BufferedWriter writer;

	public PrettyPrintVisitor() {
	}

	private void sayln(String s) {
		say(s);
		try {
			this.writer.write("\n");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void isayln(String s) {
		say("    ");
		say(s);
		try {
			this.writer.write("\n");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void say(String s) {
		try {
			this.writer.write(s);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// /////////////////////////////////////////////////////
	// statements
	@Override
	public void visit(ReturnObject s) {
		this.isayln("return-object");
		return;
	}

	@Override
	public void visit(Goto32 s) {
		this.isayln("goto/32 " + s.l.toString());
		return;
	}

	@Override
	public void visit(Iflt s) {
		this.isayln("if-lt " + s.left + ", " + s.right + ", " + s.l.toString());
		return;
	}

	@Override
	public void visit(Ifne s) {
		this.isayln("if-ne " + s.l.toString());
		return;
	}

	@Override
	public void visit(Mulint s) {
		this.isayln("imul " + s.dst + ", " + s.src1 + ", " + s.src2);
		return;
	}

	@Override
	public void visit(Invokevirtual s) {
		this.say("    invokevirtual L" + s.c + ";->" + s.f + "(");
		for (Type.T t : s.at) {
			t.accept(this);
		}
		this.say(")");
		s.rt.accept(this);
		this.sayln("");
		return;
	}

	@Override
	public void visit(Return s) {
		this.isayln("return");
		return;
	}

	@Override
	public void visit(Subint s) {
		this.isayln("sub-int " + s.dst + ", " + s.src1 + ", " + s.src2);
		return;
	}

	@Override
	public void visit(LabelJ s) {
		this.sayln(":" + s.l.toString());
		return;
	}

	@Override
	public void visit(Const s) {
		this.isayln("const " + s.dst + ", " + s.i);
		return;
	}

	@Override
	public void visit(NewInstance s) {
		this.isayln("new-instance " + s.dst + ", " + s.c);
		this.isayln("invoke-direct {" + s.dst + "}, " + s.c + "-><init>()V");
		return;
	}

	@Override
	public void visit(Print s) {
		this.isayln("sget-object {" + s.stream + "}, " + "Ljava/lang/System;->out:Ljava/io/PrintStream;");
		this.isayln("invoke-virtual {" + s.stream + ", " + s.src + "}, Ljava/io/PrintStream;->println(I)V");
		return;
	}

	@Override
	public void visit(Ifnez s) {
		this.isayln("if-ne " + s.left + ", :" + s.l.toString());
		return;
	}

	@Override
	public void visit(Move16 s) {
		this.isayln("move/16 " + s.left + ", " + s.right);
		return;
	}

	@Override
	public void visit(Moveobject16 s) {
		this.isayln("move-object/16 " + s.left + s.right);
		return;
	}

	// //////////////////////////////////////////////////////
	// type
	@Override
	public void visit(ClassType t) {
		this.say("L" + t.id + ";");
	}

	@Override
	public void visit(Int t) {
		this.say("I");
	}

	@Override
	public void visit(IntArray t) {
		this.say("[I");
	}

	// //////////////////////////////////////////////////
	// dec
	@Override
	public void visit(DecSingle d) {
	}

	// //////////////////////////////////////////////////
	// method
	@Override
	public void visit(MethodSingle m) {
		this.say(".method public " + m.id + "(");
		for (Dec.T d : m.formals) {
			DecSingle dd = (DecSingle) d;
			dd.type.accept(this);
		}
		this.say(")");
		m.retType.accept(this);
		this.sayln("");
		this.sayln(".limit stack 4096");
		this.sayln(".limit locals " + (m.index + 1));

		for (Stm.T s : m.stms)
			s.accept(this);

		this.sayln(".end method");
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
		// Every class must go into its own class file.
		try {
			this.writer = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(new java.io.FileOutputStream(c.id + ".smali")));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// header
		this.sayln("; This is automatically generated by the Tiger compiler.");
		this.sayln("; Do NOT modify!\n");

		this.sayln(".class public " + c.id);
		if (c.extendss == null)
			this.sayln(".super java/lang/Object\n");
		else
			this.sayln(".super " + c.extendss);

		// fields
		for (Dec.T d : c.decs) {
			DecSingle dd = (DecSingle) d;
			this.say(".field public " + dd.id);
			dd.type.accept(this);
			this.sayln("");
		}

		// methods
		this.sayln(".method public <init>()V");
		this.isayln("aload 0");
		if (c.extendss == null)
			this.isayln("invokespecial java/lang/Object/<init>()V");
		else
			this.isayln("invokespecial " + c.extendss + "/<init>()V");
		this.isayln("return");
		this.sayln(".end method\n\n");

		for (Method.T m : c.methods) {
			m.accept(this);
		}

		try {
			this.writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return;
	}

	// main class
	@Override
	public void visit(MainClassSingle c) {
		// Every class must go into its own class file.
		try {
			this.writer = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(new java.io.FileOutputStream(c.id + ".j")));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		this.sayln("; This is automatically generated by the Tiger compiler.");
		this.sayln("; Do NOT modify!\n");

		this.sayln(".class public " + c.id);
		this.sayln(".super java/lang/Object\n");
		this.sayln(".method public static main([Ljava/lang/String;)V");
		this.isayln(".limit stack 4096");
		this.isayln(".limit locals 2");
		for (Stm.T s : c.stms)
			s.accept(this);
		this.isayln("return");
		this.sayln(".end method");

		try {
			this.writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p) {

		p.mainClass.accept(this);

		for (Class.T c : p.classes) {
			c.accept(this);
		}

	}

}
