# Basic assignments
x = 10;
y = 20;
z = 2.5;

# Basic matrix operations
A = [1, 2, 3];
B = [4, 5, 6];
C = [A, B];
D = C .+ C;
E = C .* C;

# Transpose and scalar operations
X = ones(3);
T = X';
S = T * 2.5;

# Matrix creation with nested vectors
F = [[1, 2, 3], [4, 5, 6], [7, 8, 9]];

# Built-in matrix functions
identity_matrix = eye(4);
all_ones = ones(4);
all_zeros = zeros(4);

# Accessing elements
m_elem1 = F[0, 0];
m_elem2 = F[1, 2];

# Vector element access
v_elem1 = A[2];

# Nested loops
for i = 0:2 {
    for j = 0:2 {
        print "i:", i, "j:", j;
    }
}

# Conditions
if (x < y) {
    print "x is less than y";
    z = x + y;
} else if (x == y) {
    print "x is equal to y";
} else {
    print "x is greater than y";
}

# While loop with nested conditionals
while (z > 0) {
    if (z < 5) {
        print "z is small:", z;
    } else {
        print "z is:", z;
    }
    z -= 1;
}

# Compound assignments
x += 5;
y -= 2;
z *= 2;

# Loop control
for i = 0:10 {
    if (i == 3) {
        continue;
    }
    if (i == 7) {
        break;
    }
    print "Loop index:", i;
}

# Function application
result = eye(3) * 2;
string_value = "This is a string";
print "Matrix multiplied by scalar:", result;

# Invalid function usage (should trigger error)
invalid_function = eye("three");

# Arithmetic operations with precedence
computed_value = 2 + 3 * (4 - 1) / 2;
dot_operations = A .+ B .- [3, 2, 1];

# Return statement
return result;
# Initialize matrices and perform basic operations
A = [ [1, 2, 3],
      [4, 5, 6],
      [7, 8, 9] ];
B = zeros(3);
C = eye(3) .+ ones(3);

# Assigning values and accessing matrix elements
D = A .+ B;          # Matrix addition
D[0, 1] = 10;       # Set element
x = D[0, 1];        # Access element
y = D[2, 2] * 5;    # Multiply element

# Loop with a range
for i = 0:3 {
    print i;
    for j = 0:3 {
        C[i, j] = i * j;
    }
}

# Conditional statements
if (x < 10) {
    print "x is less than 10";
} else {
    print "x is greater or equal to 10";
}

# Nested loops
for i = 0:3 {
    for j = 0:3 {
        print C[i, j];
        if (C[i, j] > 5) {
            break;   # Exit inner loop
        }
    }
}

# Function usage
M = ones(4) .+ zeros(4);

# Unary operations
negA = -A;
transposeB = B';

# Matrix initialization and print
G = [ [2.5, 3.1, 4.8],
      [1.2, 5.6, 6.7],
      [9.1, 8.3, 7.4] ];
print G;

# Return example
return x .+ y;
