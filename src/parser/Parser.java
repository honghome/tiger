package parser;

import jdk.nashorn.internal.parser.TokenKind;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

public class Parser {
	Lexer lexer;
	Token current;

	public Parser(String fname, java.io.InputStream fstream) {
		lexer = new Lexer(fname, fstream);
		current = lexer.nextToken();
	}

	// /////////////////////////////////////////////
	// utility methods to connect the lexer
	// and the parser.

	private void advance() {
		current = lexer.nextToken();
	}

	private void eatToken(Kind kind) {
		if (kind == current.kind)
			advance();
		else {
			
			System.out.println("Expects: " + kind.toString());
			System.out.println("But got: " + current.toString());
			new Exception().printStackTrace();
			System.exit(1);
		}
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
	private void parseExpList() {
		if (current.kind == Kind.TOKEN_RPAREN)
			return;
		parseExp();
		while (current.kind == Kind.TOKEN_COMMER) {
			advance();
			parseExp();
		}
		return;
	}

	// AtomExp -> (exp)
	// -> INTEGER_LITERAL
	// -> true
	// -> false
	// -> this
	// -> id
	// -> new int [exp]
	// -> new id ()
	private void parseAtomExp() {
		switch (current.kind) {
		case TOKEN_LPAREN:
			advance();
			parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			return;
		case TOKEN_NUM:
			advance();
			return;
		case TOKEN_TRUE:
			advance();
			return;
		case TOKEN_FALSE:
			advance();
			return;
		case TOKEN_THIS:
			advance();
			return;
		case TOKEN_ID:
			advance();
			return;
		case TOKEN_NEW:
			advance();
			switch (current.kind) {
			case TOKEN_INT:
				advance();
				eatToken(Kind.TOKEN_LBRACK);
				parseExp();
				eatToken(Kind.TOKEN_RBRACK);
				return;
			case TOKEN_ID:
				advance();
				eatToken(Kind.TOKEN_LPAREN);
				eatToken(Kind.TOKEN_RPAREN);
				return;
			default:
				error();
				return;
			}
		default:
			error();
			return;
		}
	}

	// NotExp -> AtomExp
	// -> AtomExp .id (expList)
	// -> AtomExp [exp]
	// -> AtomExp .length
	private void parseNotExp() {
		parseAtomExp();
		//System.out.println("kind >>> " + current.kind);
		while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
			if (current.kind == Kind.TOKEN_DOT) {
				advance();
				if (current.kind == Kind.TOKEN_LENGTH) {
					advance();
					return;
				}
				eatToken(Kind.TOKEN_ID);
				eatToken(Kind.TOKEN_LPAREN);
				parseExpList();
				eatToken(Kind.TOKEN_RPAREN);
			} else {
				advance();
				parseExp();
				eatToken(Kind.TOKEN_RBRACK);
			}
		}
		return;
	}

	// TimesExp -> ! TimesExp
	// -> NotExp
	private void parseTimesExp() {
		while (current.kind == Kind.TOKEN_NOT) {
			advance();
		}
		parseNotExp();
		return;
	}

	// AddSubExp -> TimesExp * TimesExp
	// -> TimesExp
	private void parseAddSubExp() {
		parseTimesExp();
		while (current.kind == Kind.TOKEN_TIMES) {
			advance();
			parseTimesExp();
		}
		return;
	}

	// LtExp -> AddSubExp + AddSubExp
	// -> AddSubExp - AddSubExp
	// -> AddSubExp
	private void parseLtExp() {
		parseAddSubExp();
		while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
			advance();
			parseAddSubExp();
		}
		return;
	}

	// AndExp -> LtExp < LtExp
	// -> LtExp
	private void parseAndExp() {
		parseLtExp();
		while (current.kind == Kind.TOKEN_LT) {
			advance();
			parseLtExp();
		}
		return;
	}

	// Exp -> AndExp && AndExp
	// -> AndExp
	private void parseExp() {
		parseAndExp();
		while (current.kind == Kind.TOKEN_AND) {
			advance();
			parseAndExp();
		}
		return;
	}

	// <id> = Exp ;
	// <id> [ Exp ] = Exp ;
	private void parseAssign() {
		if (current.kind == Kind.TOKEN_LBRACK) {
			advance();
			parseExp();
			eatToken(Kind.TOKEN_RBRACK);
		}
		eatToken(Kind.TOKEN_ASSIGN);
		parseExp();
		eatToken(Kind.TOKEN_SEMI);
	}
	// Statement -> { Statement* }
	// -> if ( Exp ) Statement else Statement
	// -> while ( Exp ) Statement
	// -> System.out.println ( Exp ) ;
	// -> id = Exp ;
	// -> id [ Exp ]= Exp ;
	private void parseStatement() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a statement.
		if (current.kind == Kind.TOKEN_LBRACE) {
			advance();
			parseStatements();
			eatToken(Kind.TOKEN_RBRACE);
		} else if (current.kind == Kind.TOKEN_IF) {
			advance();
			eatToken(Kind.TOKEN_LPAREN);
			parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			parseStatement();
			eatToken(Kind.TOKEN_ELSE);
			parseStatement();
		} else if (current.kind == Kind.TOKEN_WHILE) {
			advance();
			eatToken(Kind.TOKEN_LPAREN);
			parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			parseStatement();
		} else if (current.kind == Kind.TOKEN_SYSTEM) {
			advance();
			eatToken(Kind.TOKEN_DOT);
			eatToken(Kind.TOKEN_OUT);
			eatToken(Kind.TOKEN_DOT);
			eatToken(Kind.TOKEN_PRINTLN);
			eatToken(Kind.TOKEN_LPAREN);
			parseExp();
			eatToken(Kind.TOKEN_RPAREN);
			eatToken(Kind.TOKEN_SEMI);
		} else if (current.kind == Kind.TOKEN_ID) {
			advance();
			parseAssign();
		} else {
			error();
			return;
		}
	}

	// Statements -> Statement Statements
	// ->
	private void parseStatements() {
		while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF || current.kind == Kind.TOKEN_WHILE
				|| current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
			parseStatement();
		}
		return;
	}

	// Type -> int []
	// -> boolean
	// -> int
	// -> id
	private void parseType() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a type.
		if (current.kind == Kind.TOKEN_INT) {
			advance();
			if (current.kind == Kind.TOKEN_LBRACK) {
				advance();
				eatToken(Kind.TOKEN_RBRACK);
			}
		} else if (current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) {
			advance();
		} else {
			error();
			return;
		}
	}

	// handle conflict:
	// VarDecl -> Type id ;
	// Type -> id
	// Statement -> id = Exp ;
	// Statement -> id [ Exp ] = Exp ;
	// return 1 if Dec or 0 if Sta
	private int parseDecOrSta() {
		eatToken(Kind.TOKEN_ID);
		if (current.kind == Kind.TOKEN_ID) {
			// VarDecl
			advance();
			eatToken(Kind.TOKEN_SEMI);
			return 1;
		} else {
			parseAssign();
			return 0;
		}
	}
	// VarDecl -> Type id ;
	private void parseVarDecl() {
		// to parse the "Type" nonterminal in this method, instead of writing
		// a fresh one.
		parseType();
		eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_SEMI);
		return;
	}

	// VarDecls -> VarDecl VarDecls
	// ->
	private void parseVarDecls() {
		while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) {
			if (current.kind == Kind.TOKEN_ID) {
				if (parseDecOrSta() == 0) {
					// get Statement
					break;
				}
			} else {
				parseVarDecl();
			}
		}
		return;
	}

	// FormalList -> Type id FormalRest*
	// ->
	// FormalRest -> , Type id
	private void parseFormalList() {
		if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) {
			parseType();
			eatToken(Kind.TOKEN_ID);
			while (current.kind == Kind.TOKEN_COMMER) {
				advance();
				parseType();
				eatToken(Kind.TOKEN_ID);
			}
		}
		return;
	}

	// Method -> public Type id ( FormalList )
	// { VarDecl* Statement* return Exp ;}
	private void parseMethod() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a method.
		eatToken(Kind.TOKEN_PUBLIC);
		parseType();
		eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_LPAREN);
		parseFormalList();
		eatToken(Kind.TOKEN_RPAREN);
		eatToken(Kind.TOKEN_LBRACE);
		parseVarDecls();
		parseStatements();
		eatToken(Kind.TOKEN_RETURN);
		parseExp();
		eatToken(Kind.TOKEN_SEMI);
		eatToken(Kind.TOKEN_RBRACE);
		return;
	}

	// MethodDecls -> MethodDecl MethodDecls
	// ->
	private void parseMethodDecls() {
		while (current.kind == Kind.TOKEN_PUBLIC) {
			parseMethod();
		}
		return;
	}

	// ClassDecl -> class id { VarDecl* MethodDecl* }
	// -> class id extends id { VarDecl* MethodDecl* }
	private void parseClassDecl() {
		eatToken(Kind.TOKEN_CLASS);
		eatToken(Kind.TOKEN_ID);
		if (current.kind == Kind.TOKEN_EXTENDS) {
			advance();
			eatToken(Kind.TOKEN_ID);
		}
		eatToken(Kind.TOKEN_LBRACE);
		parseVarDecls();
		parseMethodDecls();
		eatToken(Kind.TOKEN_RBRACE);
		return;
	}

	// ClassDecls -> ClassDecl ClassDecls
	// ->
	private void parseClassDecls() {
		while (current.kind == Kind.TOKEN_CLASS) {
			parseClassDecl();
		}
		return;
	}

	// MainClass -> class id
	// {
	// public static void main ( String [] id )
	// {
	// Statement
	// }
	// }
	private void parseMainClass() {
		// Lab1. Exercise 4: Fill in the missing code
		// to parse a main class as described by the
		// grammar above.
		eatToken(Kind.TOKEN_CLASS);
		eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_LBRACE);
		eatToken(Kind.TOKEN_PUBLIC);
		eatToken(Kind.TOKEN_STATIC);
		eatToken(Kind.TOKEN_VOID);
		eatToken(Kind.TOKEN_MAIN);
		eatToken(Kind.TOKEN_LPAREN);
		eatToken(Kind.TOKEN_STRING);
		eatToken(Kind.TOKEN_LBRACK);
		eatToken(Kind.TOKEN_RBRACK);
		eatToken(Kind.TOKEN_ID);
		eatToken(Kind.TOKEN_RPAREN);
		eatToken(Kind.TOKEN_LBRACE);
		parseStatements();
		eatToken(Kind.TOKEN_RBRACE);
		eatToken(Kind.TOKEN_RBRACE);
	}

	// Program -> MainClass ClassDecl*
	private void parseProgram() {
		parseMainClass();
		parseClassDecls();
		eatToken(Kind.TOKEN_EOF);
		return;
	}

	public void parse() {
		parseProgram();
		return;
	}
}
