package elaborator;

import java.util.LinkedList;

import ast.Ast.Class;
import ast.Ast.Exp.False;
import ast.Ast.Exp.Id;
import ast.Ast.Exp.Length;
import ast.Ast.Exp.Lt;
import ast.Ast.Exp.NewIntArray;
import ast.Ast.Exp.NewObject;
import ast.Ast.Exp.Not;
import ast.Ast.Exp.Num;
import ast.Ast.Exp.Sub;
import ast.Ast.Exp.This;
import ast.Ast.Exp.Times;
import ast.Ast.Exp.True;
import ast.Ast.Exp.ExpBlock;
import ast.Ast.MainClass;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec;
import ast.Ast.Exp;
import ast.Ast.Exp.Add;
import ast.Ast.Exp.And;
import ast.Ast.Exp.ArraySelect;
import ast.Ast.Exp.Call;
import ast.Ast.Method;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.AssignArray;
import ast.Ast.Stm.Block;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;
import ast.Ast.Stm.While;
import ast.Ast.Type;
import ast.Ast.Type.ClassType;
import control.Control.ConAst;

public class ElaboratorVisitor implements ast.Visitor {
	public ClassTable classTable; // symbol table for class
	public MethodTable methodTable; // symbol table for each method
	public String currentClass; // the class name being elaborated
	public Type.T type; // type of the expression being elaborated

	public ElaboratorVisitor() {
		this.classTable = new ClassTable();
		this.methodTable = new MethodTable();
		this.currentClass = null;
		this.type = null;
	}

	private void error(String errorMsg) {
		System.out.println("Error: " + errorMsg);
		System.exit(1);
	}

	// check whether the classType is match
	private boolean isClassMatch(String father, String child) {
		while (child != null && !child.equals(father)) {
			child = this.classTable.get(child).extendss;
		}

		return child != null && child.equals(father);
	}

	// check whether the classType is match
	private boolean isClassMatch(Type.T father, Type.T child) {
		if (father instanceof Type.ClassType && child instanceof Type.ClassType) {
			String fatherName = ((Type.ClassType) father).id;
			String childName = ((Type.ClassType) child).id;

			return isClassMatch(fatherName, childName);
		}
		return false;
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e) {
		e.left.accept(this);
		Type.T leftty = this.type;
		e.right.accept(this);
		if (!this.type.toString().equals(leftty.toString()))
			error("Add Exp : type mismatch");
		this.type = new Type.Int();
	}

	@Override
	public void visit(And e) {
		e.left.accept(this);
		if (!this.type.toString().equals("@boolean"))
			error("And Exp : left exp isn't boolean");
		e.right.accept(this);
		if (!this.type.toString().equals("@boolean"))
			error("And Exp : right exp isnt' boolean");
		this.type = new Type.Boolean();
	}

	@Override
	public void visit(ArraySelect e) {
		e.array.accept(this);
		if (!this.type.toString().equals("@int[]"))
			error("ArraySelect Exp : array isn't int but " + this.type.toString());
		e.index.accept(this);
		if (!this.type.toString().equals("@int"))
			error("ArraySelect Exp : index isn't int");
		this.type = new Type.Int();
	}

	@Override
	public void visit(Call e) {
		Type.T leftty;
		Type.ClassType ty = null;

		e.exp.accept(this);
		leftty = this.type;
		if (leftty instanceof ClassType) {
			ty = (ClassType) leftty;
			e.type = ty.id;
		} else
			error("Call exp : e.exp isn't ClassType");
		MethodType mty = this.classTable.getm(ty.id, e.id);
		java.util.LinkedList<Type.T> argsty = new LinkedList<Type.T>();
		for (Exp.T a : e.args) {
			a.accept(this);
			argsty.addLast(this.type);
		}
		if (mty.argsType.size() != argsty.size())
			error("Call exp : args size error");
		for (int i = 0; i < argsty.size(); i++) {
			Dec.DecSingle dec = (Dec.DecSingle) mty.argsType.get(i);
			Type.T arg = argsty.get(i);
			if (dec.type.toString().equals(arg.toString()))
				;
			else {
				// check class type
				if (isClassMatch(dec.type, arg)) {
					continue;
				}
				error("Call exp : args type error" + "\n\tdec.id[dec.type] : "
						+ String.format("%s[%s]", dec.id, dec.type.toString()) + "\n\targs.type : "
						+ argsty.get(i).toString());
			}

		}
		this.type = mty.retType;
		e.at = argsty;
		e.rt = this.type;
		return;
	}

	@Override
	public void visit(False e) {
		this.type = new Type.Boolean();
	}

	@Override
	public void visit(Id e) {
		// first look up the id in method table
		Type.T type = this.methodTable.get(e.id);
		// if search failed, then s.id must be a class field.
		if (type == null) {
			type = this.classTable.get(this.currentClass, e.id);
			// mark this id as a field id, this fact will be
			// useful in later phase.
			e.isField = true;
		}
		if (type == null)
			error("Id Exp : id not find");
		this.type = type;
		// record this type on this node for future use.
		e.type = type;
		return;
	}

	@Override
	public void visit(Length e) {
		e.array.accept(this);
		if (!this.type.toString().equals("@int[]"))
			error("Length Exp : array isn't int[]");
		this.type = new Type.Int();
	}

	@Override
	public void visit(Lt e) {
		e.left.accept(this);
		Type.T ty = this.type;
		e.right.accept(this);
		if (!this.type.toString().equals(ty.toString()))
			error("Lt Exp : left and right exp isn't match");
		this.type = new Type.Boolean();
		return;
	}

	@Override
	public void visit(NewIntArray e) {
		this.type = new Type.IntArray();
	}

	@Override
	public void visit(NewObject e) {
		this.type = new Type.ClassType(e.id);
		return;
	}

	@Override
	public void visit(Not e) {
		e.exp.accept(this);
		if (!this.type.toString().equals("@boolean"))
			error("Not Exp : exp not bool");
	}

	@Override
	public void visit(Num e) {
		this.type = new Type.Int();
		return;
	}

	@Override
	public void visit(Sub e) {
		e.left.accept(this);
		Type.T leftty = this.type;
		e.right.accept(this);
		if (!this.type.toString().equals(leftty.toString()))
			error("Sub Exp : left and righ exp not match");
		this.type = new Type.Int();
		return;
	}

	@Override
	public void visit(This e) {
		this.type = new Type.ClassType(this.currentClass);
		return;
	}

	@Override
	public void visit(Times e) {
		e.left.accept(this);
		Type.T leftty = this.type;
		e.right.accept(this);
		if (!this.type.toString().equals(leftty.toString()))
			error("Times Exp : left and right exp not match");
		this.type = new Type.Int();
		return;
	}

	@Override
	public void visit(True e) {
		this.type = new Type.Boolean();
	}

	@Override
	public void visit(ExpBlock e) {
		e.exp.accept(this);
	}

	// statements
	@Override
	public void visit(Assign s) {
		// first look up the id in method table
		Type.T type = this.methodTable.get(s.id);
		// if search failed, then s.id must
		if (type == null)
			type = this.classTable.get(this.currentClass, s.id);
		if (type == null)
			error("Assign Stm : id not find");
		s.exp.accept(this);
		s.type = type;
		if (!this.type.toString().equals(type.toString())) {
			if (!isClassMatch(type, this.type))
				error("Assign Stm : type not math");
		}

		return;
	}

	@Override
	public void visit(AssignArray s) {
		s.index.accept(this);
		if (!this.type.toString().equals("@int"))
			error("AssignArray Stm : index isn't int");

		s.exp.accept(this);
		if (!this.type.toString().equals("@int"))
			error("AssignArray Stm : exp isn't int");
		new Id(s.id).accept(this);
		if (!this.type.toString().equals("@int[]"))
			error("AssignArray Stm : id isn't int but " + this.type.toString());
		this.type = new Type.Int();
	}

	@Override
	public void visit(Block s) {
		for (Stm.T stm : s.stms)
			stm.accept(this);
	}

	@Override
	public void visit(If s) {
		s.condition.accept(this);
		if (!this.type.toString().equals("@boolean"))
			error("If Stm : condition isn't bool");
		s.thenn.accept(this);
		s.elsee.accept(this);
		return;
	}

	@Override
	public void visit(Print s) {
		s.exp.accept(this);
		if (!this.type.toString().equals("@int"))
			error("Print Stm : exp isn't int");
		return;
	}

	@Override
	public void visit(While s) {
		s.condition.accept(this);
		if (!this.type.toString().equals("@boolean"))
			error("While Stm : condition isn't bool");
		s.body.accept(this);
	}

	// type
	@Override
	public void visit(Type.Boolean t) {
		error("what fuck : in visit Type.Boolean");
	}

	@Override
	public void visit(Type.ClassType t) {
		error("what fuck : in visit Type.ClassType");
	}

	@Override
	public void visit(Type.Int t) {
		error("what fuck : in visit Type.Int");
	}

	@Override
	public void visit(Type.IntArray t) {
		error("what fuck : in visit Type.IntArray");
	}

	// dec
	@Override
	public void visit(Dec.DecSingle d) {
		error("what fuck : in visit Dec.DecSingle");
	}

	// method
	@Override
	public void visit(Method.MethodSingle m) {
		// construct the method table
		this.methodTable.put(m.formals, m.locals);

		if (ConAst.elabMethodTable)
			this.methodTable.dump();

		for (Stm.T s : m.stms)
			s.accept(this);
		m.retExp.accept(this);
		return;
	}

	// class
	@Override
	public void visit(Class.ClassSingle c) {
		this.currentClass = c.id;

		for (Method.T m : c.methods) {
			m.accept(this);
		}
		return;
	}

	// main class
	@Override
	public void visit(MainClass.MainClassSingle c) {
		this.currentClass = c.id;
		// "main" has an argument "arg" of type "String[]", but
		// one has no chance to use it. So it's safe to skip it...

		c.stm.accept(this);
		return;
	}

	// ////////////////////////////////////////////////////////
	// step 1: build class table
	// class table for Main class
	private void buildMainClass(MainClass.MainClassSingle main) {
		this.classTable.put(main.id, new ClassBinding(null));
	}

	// class table for normal classes
	private void buildClass(ClassSingle c) {
		this.classTable.put(c.id, new ClassBinding(c.extendss));
		for (Dec.T dec : c.decs) {
			Dec.DecSingle d = (Dec.DecSingle) dec;
			this.classTable.put(c.id, d.id, d.type);
		}
		for (Method.T method : c.methods) {
			MethodSingle m = (MethodSingle) method;
			this.classTable.put(c.id, m.id, new MethodType(m.retType, m.formals));
		}
	}

	// step 1: end
	// ///////////////////////////////////////////////////

	// program
	@Override
	public void visit(ProgramSingle p) {
		// ////////////////////////////////////////////////
		// step 1: build a symbol table for class (the class table)
		// a class table is a mapping from class names to class bindings
		// classTable: className -> ClassBinding{extends, fields, methods}
		buildMainClass((MainClass.MainClassSingle) p.mainClass);
		for (Class.T c : p.classes) {
			buildClass((ClassSingle) c);
		}

		// we can double check that the class table is OK!
		if (control.Control.ConAst.elabClassTable) {
			this.classTable.dump();
		}

		// ////////////////////////////////////////////////
		// step 2: elaborate each class in turn, under the class table
		// built above.
		p.mainClass.accept(this);
		for (Class.T c : p.classes) {
			c.accept(this);
		}

	}
}
