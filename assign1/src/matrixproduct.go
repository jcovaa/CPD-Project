package main

import (
	"fmt"
	"time"
)

func OnMult(m_ar int, m_br int) {
	// Variable declaration
	var temp float64

	// Creating matrix
	// make() allocates and initializes memory
	pha := make([]float64, m_ar*m_ar)
	phb := make([]float64, m_ar*m_ar)
	phc := make([]float64, m_ar*m_ar)

	// Initializing the pha matrix with 1's
	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_ar; j++ {
			pha[i*m_ar+j] = 1.0
		}
	}

	// Initializing the phb matrix
	for i := 0; i < m_br; i++ {
		for j := 0; j < m_br; j++ {
			phb[i*m_br+j] = float64(i + 1)
		}
	}

	// Starting clock
	start := time.Now()

	// Calculating the multiplication
	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_br; j++ {
			temp = 0
			for k := 0; k < m_ar; k++ {
				temp += pha[i*m_ar+k] * phb[k*m_br+j]
			}
			phc[i*m_ar+j] = temp
		}
	}

	// Calculate time
	duration := time.Since(start).Seconds()
	fmt.Printf("Time: %3.3f seconds\n", duration)

	fmt.Println("Result matrix: ")
	for i := 0; i < 1; i++ {
		for j := 0; j < min(10, m_br); j++ {
			fmt.Print(phc[j], " ")
		}
	}
	fmt.Println()
}

func OnMultLine(m_ar int, m_br int) {
	// Variable declaration
	var temp float64

	// Creating matrix
	// make() allocates and initializes memory
	pha := make([]float64, m_ar*m_ar)
	phb := make([]float64, m_ar*m_ar)
	phc := make([]float64, m_ar*m_ar)

	// Initializing the pha matrix with 1's
	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_ar; j++ {
			pha[i*m_ar+j] = 1.0
		}
	}

	// Initializing the phb matrix
	for i := 0; i < m_br; i++ {
		for j := 0; j < m_br; j++ {
			phb[i*m_br+j] = float64(i + 1)
		}
	}

	// Starting clock
	start := time.Now()

	// Calculating the multiplication
	for i := 0; i < m_ar; i++ {
		for k := 0; k < m_ar; k++ {
			temp = pha[i*m_ar+k]
			for j := 0; j < m_br; j++ {
				phc[i*m_ar+j] += temp * phb[k*m_br+j]
			}
		}
	}

	// Calculate time
	duration := time.Since(start).Seconds()
	fmt.Printf("Time: %3.3f seconds\n", duration)

	fmt.Println("Result matrix: ")
	for i := 0; i < 1; i++ {
		for j := 0; j < min(10, m_br); j++ {
			fmt.Print(phc[j], " ")
		}
	}
	fmt.Println()
}

func OnMultBlock(m_ar int, m_br int, bkSize int) {
	// Variable declaration
	var temp float64

	// Creating matrix
	// make() allocates and initializes memory
	pha := make([]float64, m_ar*m_ar)
	phb := make([]float64, m_ar*m_ar)
	phc := make([]float64, m_ar*m_ar)

	// Initializing the pha matrix with 1's
	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_ar; j++ {
			pha[i*m_ar+j] = 1.0
		}
	}

	// Initializing the phb matrix
	for i := 0; i < m_br; i++ {
		for j := 0; j < m_br; j++ {
			phb[i*m_br+j] = float64(i + 1)
		}
	}

	// Starting clock
	start := time.Now()

	// Calculating the multiplication
	for a := 0; a < m_ar; a += bkSize {
		for b := 0; b < m_br; b += bkSize {
			for c := 0; c < m_ar; c += bkSize {

				for i := a; i < a+bkSize; i++ {
					for k := c; k < c+bkSize; k++ {
						temp = pha[i*m_ar+k]
						for j := b; j < b+bkSize; j++ {
							phc[i*m_ar+j] += temp * phb[k*m_br+j]
						}
					}
				}

			}
		}
	}

	// Calculate time
	duration := time.Since(start).Seconds()
	fmt.Printf("Time: %3.3f seconds\n", duration)

	fmt.Println("Result matrix: ")
	for i := 0; i < 1; i++ {
		for j := 0; j < min(10, m_br); j++ {
			fmt.Print(phc[j], " ")
		}
	}
	fmt.Println()
}

func main() {
	var lin, col, bk int
	var op int

	for {
		fmt.Print("1. Multiplication\n2. Line Multiplication\n3. Block Multiplication\n0. Exit\nSelection?: ")
		fmt.Scan(&op)

		if op == 0 {
			break
		}

		fmt.Print("Dimensions: lins=cols ? ")
		fmt.Scan(&lin)
		col = lin

		switch op {
		case 1:
			OnMult(lin, col)
		case 2:
			OnMultLine(lin, col)
		case 3:
			fmt.Print("Block Size? ")
			fmt.Scan(&bk)
			OnMultBlock(lin, col, bk)
		}
	}
}
