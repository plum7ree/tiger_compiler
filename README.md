# Tiger Compiler
## Files
`src/main/java` : Main source code  
`src/main/antlr` : ANTLR grammar files  
`grammar.bnf` : hand-modified grammar  

## Building
```
$ make
```
After building, the ANTLR-generated parser code is saved to `build/generated-src/antlr/` and the final executable is saved to `build/libs/tiger.jar`.

## Running
Run the compiler by invoking the executable jar and passing the source code filename as an argument. Optional flags can be set to print the symbol table or token stream.
```
$ java -jar build/libs/tiger.jar
Usage: <main class> [--cfg-liveness] [--dot-cfg] [--dot-web] [--run] [--symtab]
                    [--tokens] [-o=<outFile>] [--ralloc=<rallocMode>] <file>
      <file>            tiger source file
      --cfg-liveness    include liveness sets in CFG output
      --dot-cfg         emit CFG as 'dot' file for every function in program
      --dot-web         emit web interference graph as 'dot' file for every
                          function in program
  -o, --out=<outFile>   MIPS assembly output file
      --ralloc=<rallocMode>
                        register allocation mode (BRIGGS, NAIVE)
                          Default: BRIGGS
      --run             run SPIM simulator after compilation
      --symtab          print symbol table after parse
      --tokens          print token stream from scanner and parser
```

## Design
### Frontend
The symbol table is structured as a stack of scope objects (backed by hash maps). Scopes are pushed and popped as the parse tree is traversed. The symbol table can store variables, function definitions, type definitions, constants, temporaries, and labels.

The symbol table is initially populated using an ANTLR listener, which adds variables, function definitions, and type definitions to the symbol table. This phase also creates the scopes in the symbol table, and decorates the parse tree with scope references so that the scopes can be rebound in later parse tree walks.

The next phase performs semantic type checking with an ANTLR listener. It begins at leaf nodes (identifiers and constants) and decorates the parse tree with their symbols and types. It propagates type information upward through binary operator results and checks type compatibility between the expression and the statement where it is used.

The IR code generation is implemented with the ANTLR visitor pattern, as it provides control over which branches of the parse tree are explored. It is implemented recursively, with lowering functions defined for all of the language constructs. The functions may return a symbol representing a temporary register holding the result, if applicable.

## Sample output
```
$ java -jar build/libs/tiger.jar --dot-cfg --dot-web --cfg-liveness -o factorial.s test/factorial.tiger --run
successful parse
# start_function main
int main():
  assign r, 1
  call _t3, fact, 5
  assign r, _t3
  call _t4, printi, r
  assign r, _t4
  return 0
# end_function main

# start_function fact
int fact(int):
  beq n, 1, _label0
  goto _label1
_label0:
  return 1
_label1:
  sub _t1, n, 1
  call _t0, fact, _t1
  assign r, _t0
  mul _t2, n, r
  return _t2
# end_function fact
successful compile

===== Block: 1 =====
assign r, 1
call _t3, fact, 5
assign r, _t3
call _t4, printi, r
assign r, _t4
return 0
#### IN SET ####
#### INTERNAL ####
Symbol: r, Spill: 1, Hash: 376416077, Range: 1
Symbol: _t4, Spill: 2, Hash: 1485697819, Range: 2
Symbol: _t3, Spill: 2, Hash: 1089504328, Range: 2
Symbol: r, Spill: 1, Hash: 867398280, Range: 1
Symbol: r, Spill: 2, Hash: 660879561, Range: 2
#### OUT SET ####

Color Nodes: 
r: 0
_t4: 0
_t3: 0
r: 0
r: 0
Spilled Nodes: 

===== Block: 1 =====
beq n, 1, _label0
#### IN SET ####
#### INTERNAL ####
#### OUT SET ####

===== Block: 2 =====
goto _label1
#### IN SET ####
#### INTERNAL ####
#### OUT SET ####

===== Block: 3 =====
_label0:
return 1
#### IN SET ####
#### INTERNAL ####
#### OUT SET ####

===== Block: 4 =====
_label1:
sub _t1, n, 1
call _t0, fact, _t1
assign r, _t0
mul _t2, n, r
return _t2
#### IN SET ####
#### INTERNAL ####
Symbol: _t2, Spill: 2, Hash: 1889248251, Range: 2
Symbol: _t0, Spill: 2, Hash: 97652294, Range: 2
Symbol: _t1, Spill: 2, Hash: 1935972447, Range: 2
#### OUT SET ####

Color Nodes: 
_t2: 0
_t0: 0
_t1: 0
Spilled Nodes: 

cfg saved to test/cfg.main.dot
cfg saved to test/cfg.fact.dot

web saved to test/web.main.dot
web saved to test/web.fact.dot

MIPS Assembly:
# stdlib functions
printi:
li $v0, 1       # syscall number: print integer
syscall         # argument is already in $v0
li $v0, 11      # syscall number: print character
li $a0, 0x0a    # newline char
syscall
jr $ra

printf:
li $v0, 2       # syscall number: print float
mtc1 $a0, $f12
syscall
li $v0, 11      # syscall number: print character
li $a0, 0x0a    # newline char
syscall
jr $ra

_memset:
sll $t9, $t9, 2
add $t9, $t8, $t9
_memset_loop:
sw $v0, ($t8)
add $t8, $t8, 4
blt $t8, $t9, _memset_loop
jr $ra

# compiled code
main:
sub $sp, $sp, 16
move $gp, $sp
sw $ra, 12($sp)
li $t9, 1
move $t0, $t9
li $a0, 5
jal fact
move $t0, $v0
sw $t0, 0($sp)
move $a0, $t0
jal printi
move $t0, $v0
li $v0, 0
lw $ra, 12($sp)
add $sp, $sp, 16
jr $ra
fact:
sub $sp, $sp, 20
sw $ra, 16($sp)
li $t9, 1
beq $a0, $t9, _label0
j _label1
_label0:
li $v0, 1
lw $ra, 16($sp)
add $sp, $sp, 20
jr $ra
_label1:
li $t9, 1
sub $t0, $a0, $t9
sw $t0, 8($sp)
sw $a0, 0($sp)
move $a0, $t0
jal fact
lw $a0, 0($sp)
move $t0, $v0
move $t8, $t0
sw $t8, 0($gp)
lw $t9, 0($gp)
mul $t0, $a0, $t9
move $v0, $t0
lw $ra, 16($sp)
add $sp, $sp, 20
jr $ra

Starting SPIM simulator
SPIM Version 8.0 of January 8, 2010
Copyright 1990-2010, James R. Larus.
All Rights Reserved.
See the file README for a full copyright notice.
Loaded: /usr/lib/spim/exceptions.s
120
```
