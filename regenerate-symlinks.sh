# docs
rm -f README.md
ln -s docs/_docs/getting-started.md README.md
# example
rm -f example/mill
ln -s mill example/mill
rm -f example/.scalafmt.conf
ln -s .scalafmt.conf example/.scalafmt.conf
# thesis
rm -f thesis/src/alpaca.pdf
ln -s out/alpaca.pdf thesis/src/alpaca.pdf