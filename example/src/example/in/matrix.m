A = eye(3);
B = ones(3); C = A .+ B;
D = A .* B;
print C;
print D;

D = zeros(3);
D[0, 0] = 42;
#D[1:3, 2:4] = 7; # opcjonalnie dla zainteresowanych
print D;
print D[2, 2];