# puppet-to-diagram

(write explanation of the application)

(have some examples here)

(note that I'm not much experienced on Puppet, and this might no generalize to all Puppet projects)

# How to use

## Installation
``` ./tools/install.sh ```

## Configuration
Before you can generate a diagram, you have to define two configuration files: a general one and a diagram-specific one.

### General Config
By default, it is called `.puppet-to-diagram.conf` and should be located in your home directory. I should look like this:

```
path-to-puppet-project = "<path to your local Puppet project directory e.g. /home/myname/Development/puppet>"
```

### Diagram-specific Config
This will contain the configuration necessary to generate a diagram for the Puppet module you're interested in.

```
module = "<name of the module>"
parameters-configs = [
  {
    raw-name = "<name of module parameter>"
    pretty-name = "<name that will show up on the diagram>"
    arrow-direction = {
      type = "<possible values are 'in' and 'out'>"
    }
  },
  {
    raw-name = "<name of module parameter>"
    pretty-name = "<name that will show up on the diagram>"
    arrow-direction = {
      type = "<possible values are 'in' and 'out'>"
    }
  },
  (...)
]

```

On `parameter-configs` you configure which data you want to see represented on the diagram.

The `arrow-direction` configures if the arrow connected to the node representing that parameter will be incoming or outgoing.

## How to run

```
puppet-to-diagram --diagram-config ~/path/to/diagram-specific-conf.conf --environment development --server myserver.trees.com
```

Run `puppet-to-diagram --help` for details.