# docs
rm README.md
ln -s ./docs/_docs/getting-started.md README.md
# example
rm ./example/.mill
rm ./example/.scalafmt.conf
ln -s .mill ./example/.mill
ln -s .scalafmt.conf ./example/.scalafmt.conf
# thesis
rm thesis/src/alpaca.pdf
ln -s thesis/src/out/alpaca.pdf thesis/src/alpaca.pdf