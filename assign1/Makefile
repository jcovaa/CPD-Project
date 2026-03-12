CXX = g++
GO  = go

CXXFLAGS = -Wall -O2 -fopenmp
GOFLAGS  =

SRC_DIR = src
BIN_DIR = bin

CPP_BIN = $(BIN_DIR)/matrixproduct_cpp
GO_BIN  = $(BIN_DIR)/matrixproduct_go

.PHONY: all clean

all: $(CPP_BIN) $(GO_BIN)

$(CPP_BIN): $(SRC_DIR)/matrixproduct.cpp
	$(CXX) $(CXXFLAGS) $< -o $@

$(GO_BIN): $(SRC_DIR)/matrixproduct.go
	$(GO) build -o $@ $<

clean:
	rm -rf $(BIN_DIR)
