# docs
rm README.md || echo "No README.md to remove"
ln -s ./docs/_docs/getting-started.md README.md
# example
rm ./example/.mill || echo "No .mill to remove"
rm ./example/.scalafmt.conf || echo "No .scalafmt.conf to remove"
ln -s .mill ./example/.mill
ln -s .scalafmt.conf ./example/.scalafmt.conf
# thesis
rm thesis/src/alpaca.pdf || echo "No thesis/src/alpaca.pdf to remove"
ln -s thesis/src/out/alpaca.pdf thesis/src/alpaca.pdf