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
