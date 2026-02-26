package main

import (
	"fmt"
)

func OnMult(m_ar int, m_br int) {
	//var st [100]char;
}

func OnMultLine(m_ar int, m_br int) {

}

func main() {
	var lin, col int
	var op int

	fmt.Print("1. Multiplication\n2. Line Multiplication\n3. Block Multiplication\n0. Exit\nSelection?: ")
	fmt.Scan(&op)

	if op == 0 {
		return
	}

	fmt.Print("Dimensions: lins=cols ? ")
	fmt.Scan(&lin)
	fmt.Scan(&col)

	switch op {
	case 1:
		OnMult(lin, col)
	case 2:
		OnMultLine(lin, col)
	default:
		return
	}
}
