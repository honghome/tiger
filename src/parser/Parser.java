package parser;

import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;
import ast.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class Parser {
	Lexer lexer;
	Token current;
	Token backToken;

	public Parser(String fname, java.io.InputStream fstream) {
		lexer = new Lexer(fname, fstream);
		current = lexer.nextToken();
		backToken = null;
	}

	// /////////////////////////////////////////////
	// utility methods to connect the lexer
	// and the parser.

	private void advance() {
		if (backToken != null) {
			current = backToken;
			backToken = null;
		} else {
			current = lexer.nextToken();
		}
	}

	private String eatToken(Kind kind) {
		String lexeme = current.lexeme;

		if (kind == current.kind)
			advance();
		else {
			System.out.println("Expects: " + kind.toString());
			System.out.println("But got: " + current.toString());
			new Exception().printStackTrace();
			System.exit(1);
		}
		return lexeme;
	}

	private void error() {
		System.out.println("Syntax error: compilation aborting...\n");
		new Exception().printStackTrace();
		System.exit(1);
		return;
	}

	// ////////////////////////////////////////////////////////////
	// below are method for parsing.

	// A bunch of parsing methods to parse expressions. The messy
	// parts are to deal with precedence and associativity.

	// ExpList -> Exp ExpRest*
	// ->
	// ExpRest -> , Exp
	private LinkedList<Ast.Exp.T> parseExpList() {
		LinkedList<Ast.Exp.T> exps = new LinkedList<Ast.Exp.T>();

		if (current.kind != Kind.TOKEN_RPAREN) {
			exps.add(parseExp());
			while (current.kind == Kind.TOKEN_COMMER) {
				advance();
				exps.add(parseExp());
			}
		}
		
		return exps;
	}

	// AtomExp -> (exp)
	// -> INTEGER_LITERAL
	// -> true
	// -> false
	// -> this
	// -> id
	// -> new int [exp]
	// -> new id ()
	private Ast.Exp.T parseAtomExp() {
		Ast.Exp.T exp;
		String id;

		switch (current.kind) {
		case TOKEN_LPAREN:
			advance();
			exp = new Ast.Exp.ExpBlock(parseExp());
			eatToken(Kind.TOKEN_RPAREN);

			return exp;
		case TOKEN_NUM:
			int num;

			num = Integer.parseInt(eatToken(Kind.TOKEN_NUM));

			return new Ast.Exp.Num(num);
		case TOKEN_TRUE:
			advance();

			return new Ast.Exp.True();
		case TOKEN_FALSE:
			advance();

			return new Ast.Exp.False();
		case TOKEN_THIS:
			advance();

			return new Ast.Exp.This();
		case TOKEN_ID:
			return new Ast.Exp.Id(eatToken(Kind.TOKEN_ID));
		case TOKEN_NEW:
			advance();
			switch (current.kind) {
			case TOKEN_INT:
				advance();
				eatToken(Kind.TOKEN_LBRACK);
				exp = parseExp();
				eatToken(Kind.TOKEN_RBRACK);

				return new Ast.Exp.NewIntArray(exp);
			case TOKEN_ID:
				id = eatToken(Kind.TOKEN_ID);
				eatToken(Kind.TOKEN_LPAREN);
				eatToken(Kind.TOKEN_RPAREN);

				return new Ast.Exp.NewObject(id);
			default:
				error();
				return null;
			}
		default:
			error();
			return null;
		}
	}

	// NotExp -> AtomExp
	// -> AtomExp .id (expList)
	// -> AtomExp [exp]
	// -> AtomExp .length
	/*
	 * TODO how to dealwith while (current.kind == Kind.TOKEN_DOT ||
	 * current.kind == Kind.TOKEN_LBRACK)
	 */
	private Ast.Exp.T parseNotExp() {
		Ast.Exp.T exp;

		exp = parseAtomExp();
		// System.out.println("kind >>> " + current.kind);
		// while (current.kind == Kind.TOKEN_DOT || current.kind ==
		// Kind.TOKEN_LBRACK) {
		if (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
			if (current.kind == Kind.TOKEN_DOT) {
				advance();
				if (current.kind == Kind.TOKEN_LENGTH) {
					advance();

					return new Ast.Exp.Length(exp);
				}

				String id;
				LinkedList<Ast.Exp.T> args;

				id = eatToken(Kind.TOKEN_ID);
				eatToken(Kind.TOKEN_LPAREN);
				args = parseExpList();
				eatToken(Kind.TOKEN_RPAREN);

				return new Ast.Exp.Call(exp, id, args);
			} else {
				Ast.Exp.T index;

				advance();
				index = parseExp();
				eatToken(Kind.TOKEN_RBRACK);

				return new Ast.Exp.ArraySelect(exp, index);
			}
		}

		return exp;
	}

	// TimesExp -> ! TimesExp
	// -> NotExp
	private Ast.Exp.T parseTimesExp() {
		int cnt = 0;

		while (current.kind == Kind.TOKEN_NOT) {
			advance();
			cnt++;
		}

		if (cnt % 2 == 1) {
			return new Ast.Exp.Not(parseNotExp());
		} else {
			return parseNotExp();
		}
	}

	// AddSubExp -> TimesExp * TimesExp
	// -> TimesExp
	private Ast.Exp.T parseAddSubExp() {
		ArrayList<Ast.Exp.T> times = new ArrayList<Ast.Exp.T>();

		times.add(parseTimesExp());
		while (current.kind == Kind.TOKEN_TIMES) {
			advance();
			times.add(parseTimesExp());
		}
		return cstTimes(times);
	}

	private Ast.Exp.T cstTimes(ArrayList<Ast.Exp.T> times) {
		int size = times.size();
		
		switch (size) {
		case 0:
			return null;
		case 1:
			return times.get(0);
		case 2:
			return new Ast.Exp.Times(times.get(0), times.get(1));
		default:
			return new Ast.Exp.Times(times.remove(0), cstTimes(times));
		}
	}

	// LtExp -> AddSubExp + AddSubExp
	// -> AddSubExp - AddSubExp
	// -> AddSubExp
	private Ast.Exp.T parseLtExp() {
		ArrayList<Ast.Exp.T> addsubs = new ArrayList<Ast.Exp.T>();
		ArrayList<Boolean> types = new ArrayList<Boolean>();

		addsubs.add(parseAddSubExp());
		while (true) {
			if (current.kind == Kind.TOKEN_ADD) {
				types.add(true);
			} else if (current.kind == Kind.TOKEN_SUB) {
				types.add(false);
			} else {
				break;
			}
			advance();
			addsubs.add(parseAddSubExp());
		}

		return cstAddSubs(addsubs, types);
	}

	private Ast.Exp.T cstAddSubs(ArrayList<Ast.Exp.T> addsubs, ArrayList<Boolean> types) {
		int size = addsubs.size();
		
		switch (size) {
		case 0:
			return null;
		case 1:
			return addsubs.get(0);
		case 2:
			if (types.get(0) == true) {
				return new Ast.Exp.Add(addsubs.get(0), addsubs.get(1));
			} else {
				return new Ast.Exp.Sub(addsubs.get(0), addsubs.get(1));
			}
		default:
			if (types.remove(0) == true) {
				return new Ast.Exp.Add(addsubs.remove(0), cstAddSubs(addsubs, types));
			} else {
				return new Ast.Exp.Sub(addsubs.remove(0), cstAddSubs(addsubs, types));
			}
		}
	}

	// AndExp -> LtExp < LtExp
	// -> LtExp
	private Ast.Exp.T parseAndExp() {
		ArrayList<Ast.Exp.T> lts = new ArrayList<Ast.Exp.T>();

		lts.add(parseLtExp());
		while (current.kind == Kind.TOKEN_LT) {
			advance();
			lts.add(parseLtExp());
		}

		return cstLt(lts);
	}

	// LtExp < LtExp < ltExp ...
	private Ast.Exp.T cstLt(ArrayList<Ast.Exp.T> lts) {
		int size = lts.size();
		
		switch (size) {
		case 0:
			return null;
		case 1:
			return lts.get(0);
		case 2:
			return new Ast.Exp.Lt(lts.get(0), lts.get(1));
		default:
			return new Ast.Exp.Lt(lts.remove(0), cstLt(lts));
		}
	}

	// Exp -> AndExp && AndExp
	// -> AndExp
	private Ast.Exp.T parseExp() {
		ArrayList<Ast.Exp.T> ands = new ArrayList<Ast.Exp.T>();
		
		ands.add(parseAndExp());
		while (current.kind == Kind.TOKEN_AND) {
			advance();
			ands.add(parseAndExp());
		}

		return cstAnd(ands);
	}

	// andExp && AndExp && AndExp ...
	private Ast.Exp.T cstAnd(ArrayList<Ast.Exp.T> ands) {
		int size = ands.size();
		
		switch (size) {
		case 0:
			return null;
		case 1:
			return ands.get(0);
		case 2:
			return new Ast.Exp.And(ands.get(0), ands.get(1));
		default:
			return new Ast.Exp.And(ands.remove(0), cstAnd(ands));
		}
	}

	// Statement -> { Statement* }
	// -> if ( Exp ) Statement else Statement
	// -> while ( Exp ) Statement
	// -> System.out.println ( Exp ) ;
	// -> id = Exp ;
	// -> id [ Exp ]= Exp ;
	private Ast.Stm.T parseStatement() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a statement.
		if (current.kind == Kind.TOKEN_LBRACE) {
			LinkedList<Ast.Stm.T> stms;

			advance();
			stms = parseStatements();
			eatToken(Kind.TOKEN_RBRACE);

			return new Ast.Stm.Block(stms);
		} else if (current.kind == Kind.TOKEN_IF) {
			Ast.Exp.T condition;
			Ast.Stm.T thenn, elsee;

			advance();
			eatToken(Kind.TOKEN_LPAREN);
			condition = parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			thenn = parseStatement();
			eatToken(Kind.TOKEN_ELSE);
			elsee = parseStatement();

			return new Ast.Stm.If(condition, thenn, elsee);
		} else if (current.kind == Kind.TOKEN_WHILE) {
			Ast.Exp.T condition;
			Ast.Stm.T body;

			advance();
			eatToken(Kind.TOKEN_LPAREN);
			condition = parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			body = parseStatement();

			return new Ast.Stm.While(condition, body);
		} else if (current.kind == Kind.TOKEN_SYSTEM) {
			Ast.Exp.T exp;

			advance();
			eatToken(Kind.TOKEN_DOT);
			eatToken(Kind.TOKEN_OUT);
			eatToken(Kind.TOKEN_DOT);
			eatToken(Kind.TOKEN_PRINTLN);
			eatToken(Kind.TOKEN_LPAREN);
			exp = parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			eatToken(Kind.TOKEN_SEMI);

			return new Ast.Stm.Print(exp);
		} else if (current.kind == Kind.TOKEN_ID) {
			String id = eatToken(Kind.TOKEN_ID);
			Ast.Exp.T index = null, exp = null;
			
			if (current.kind == Kind.TOKEN_LBRACK) {
				advance();
				index = parseExp();
				eatToken(Kind.TOKEN_RBRACK);
			}
			eatToken(Kind.TOKEN_ASSIGN);
			exp = parseExp();
			eatToken(Kind.TOKEN_SEMI);

			if (index == null)
				return new Ast.Stm.Assign(id, exp);
			return new Ast.Stm.AssignArray(id, index, exp);
		} else {
			error();
			return null;
		}
	}

	// Statements -> Statement Statements
	// ->
	private LinkedList<Ast.Stm.T> parseStatements() {
		LinkedList<Ast.Stm.T> stms = new LinkedList<Ast.Stm.T>();

		while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF || current.kind == Kind.TOKEN_WHILE
				|| current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
			stms.add(parseStatement());
		}
		return stms;
	}

	// Type -> int []
	// -> boolean
	// -> int
	// -> id
	private Ast.Type.T parseType() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a type.
		if (current.kind == Kind.TOKEN_INT) {
			advance();
			if (current.kind == Kind.TOKEN_LBRACK) {
				advance();
				eatToken(Kind.TOKEN_RBRACK);
				return new Ast.Type.IntArray();
			} else {
				return new Ast.Type.Int();
			}
		} else if (current.kind == Kind.TOKEN_BOOLEAN) {
			advance();
			return new Ast.Type.Boolean();
		} else if (current.kind == Kind.TOKEN_ID) {
			return new Ast.Type.ClassType(eatToken(Kind.TOKEN_ID));
		} else {
			error();
			return null;
		}
	}

	// VarDecl -> Type id ;
	private Ast.Dec.T parseVarDecl() {
		// to parse the "Type" nonterminal in this method, instead of writing
		// a fresh one.
		Token preToken = (current.kind == Kind.TOKEN_ID ? current : null);
		Ast.Type.T type = parseType();
		
		//System.out.println("bacntoken >>> " + this.backToken);
		//System.out.println("type >>> " + type.toString());
		if (preToken != null && current.kind != Kind.TOKEN_ID) {
			this.backToken = current;
			current = preToken;
			return null;
		}

		String id = eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_SEMI);

		return new Ast.Dec.DecSingle(type, id);
	}

	// VarDecls -> VarDecl VarDecls
	// ->
	private LinkedList<Ast.Dec.T> parseVarDecls() {
		LinkedList<Ast.Dec.T> decs = new LinkedList<Ast.Dec.T>();
		Ast.Dec.T dec = null;

		while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) {
			dec = parseVarDecl();
			if (dec == null)
				break;
			decs.add(dec);
		}

		return decs;
	}

	// FormalList -> Type id FormalRest*
	// ->
	// FormalRest -> , Type id
	private LinkedList<Ast.Dec.T> parseFormalList() {
		LinkedList<Ast.Dec.T> formals = new LinkedList<Ast.Dec.T>();

		if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) {
			formals.add(new Ast.Dec.DecSingle(parseType(), eatToken(Kind.TOKEN_ID)));
			while (current.kind == Kind.TOKEN_COMMER) {
				advance();
				formals.add(new Ast.Dec.DecSingle(parseType(), eatToken(Kind.TOKEN_ID)));
			}
		}

		return formals;
	}

	// Method -> public Type id ( FormalList )
	// { VarDecl* Statement* return Exp ;}
	private Ast.Method.T parseMethod() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a method.
		Ast.Type.T retType;
		String id;
		LinkedList<Ast.Dec.T> formals;
		LinkedList<Ast.Dec.T> locals;
		LinkedList<Ast.Stm.T> stms;
		Ast.Exp.T retExp;

		eatToken(Kind.TOKEN_PUBLIC);
		retType = parseType();
		id = eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_LPAREN);
		formals = parseFormalList();
		eatToken(Kind.TOKEN_RPAREN);
		eatToken(Kind.TOKEN_LBRACE);
		locals = parseVarDecls();
		stms = parseStatements();
		eatToken(Kind.TOKEN_RETURN);
		retExp = parseExp();
		eatToken(Kind.TOKEN_SEMI);
		eatToken(Kind.TOKEN_RBRACE);

		return new Ast.Method.MethodSingle(retType, id, formals, locals, stms, retExp);
	}

	// MethodDecls -> MethodDecl MethodDecls
	// ->
	private LinkedList<Ast.Method.T> parseMethodDecls() {
		LinkedList<Ast.Method.T> methods = new LinkedList<Ast.Method.T>();

		while (current.kind == Kind.TOKEN_PUBLIC) {
			methods.add(parseMethod());
		}
		return methods;
	}

	// ClassDecl -> class id { VarDecl* MethodDecl* }
	// -> class id extends id { VarDecl* MethodDecl* }
	private Ast.Class.T parseClassDecl() {
		String id, extendss = null;
		LinkedList<Ast.Dec.T> decs;
		LinkedList<Ast.Method.T> methods;

		eatToken(Kind.TOKEN_CLASS);
		id = eatToken(Kind.TOKEN_ID);
		if (current.kind == Kind.TOKEN_EXTENDS) {
			advance();
			extendss = eatToken(Kind.TOKEN_ID);
		}
		eatToken(Kind.TOKEN_LBRACE);
		decs = parseVarDecls();
		methods = parseMethodDecls();
		eatToken(Kind.TOKEN_RBRACE);

		return new Ast.Class.ClassSingle(id, extendss, decs, methods);
	}

	// ClassDecls -> ClassDecl ClassDecls
	// ->
	private LinkedList<Ast.Class.T> parseClassDecls() {
		LinkedList<Ast.Class.T> classes = new LinkedList<Ast.Class.T>();

		while (current.kind == Kind.TOKEN_CLASS) {
			classes.add(parseClassDecl());
		}
		return classes;
	}

	// MainClass -> class id
	// {
	// public static void main ( String [] id )
	// {
	// Statement
	// }
	// }
	private Ast.MainClass.T parseMainClass() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a main class as described by the
		// grammar above.

		String id, arg;
		ast.Ast.Stm.T stm;

		eatToken(Kind.TOKEN_CLASS);
		id = eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_LBRACE);
		eatToken(Kind.TOKEN_PUBLIC);
		eatToken(Kind.TOKEN_STATIC);
		eatToken(Kind.TOKEN_VOID);
		eatToken(Kind.TOKEN_MAIN);
		eatToken(Kind.TOKEN_LPAREN);
		eatToken(Kind.TOKEN_STRING);
		eatToken(Kind.TOKEN_LBRACK);
		eatToken(Kind.TOKEN_RBRACK);
		arg = eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_RPAREN);
		eatToken(Kind.TOKEN_LBRACE);
		stm = parseStatement();
		eatToken(Kind.TOKEN_RBRACE);
		eatToken(Kind.TOKEN_RBRACE);

		return new ast.Ast.MainClass.MainClassSingle(id, arg, stm);
	}

	// Program -> MainClass ClassDecl*
	private Ast.Program.T parseProgram() {
		Ast.MainClass.T mainClass;
		LinkedList<Ast.Class.T> classes;

		mainClass = parseMainClass();
		classes = parseClassDecls();
		eatToken(Kind.TOKEN_EOF);

		return new Ast.Program.ProgramSingle(mainClass, classes);
	}

	public ast.Ast.Program.T parse() {
		return parseProgram();
	}
}
