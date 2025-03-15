To prepare your project for IDEs, and in general any BSP client, you can also run this command to generate the BSP
configuration files:

```bash
./mill mill.bsp.BSP/install
```

To generate IntelliJ IDEA project files into .idea/, run:

```bash
./mill mill.idea.GenIdea/
```

After the files are generated, you can open the folder in IntelliJ to load the project into your IDE. If you make
changes to your Mill build.mill, you can update the project config those updates by running ./mill mill.idea.GenIdea/
again.