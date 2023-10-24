# ICM Pipeline ECPF Converter

This tool has been developed to facilitate the comparison of two (or more) ICM pipeline files.

This tool takes a list of pipeline files and then, for each start node, produces a JSON-like file which can be  easily compared using `diff` or similar tools.

Example of an ECPF file (generated from `ViewCart-RemoveProduct` start node):

```
{
  type: [StartNode]
  name: [RemoveProduct]
  is_strict: [null]
  call_mode: [null]
  visibility: [null]
  is_secure: [null]
  session_mode: [null]
  parameters [
  ]
  successors [
    {
      name: [next]
      transaction_mode: [null]
      next_node_type: [CallNode]
    }
  ]
}
{
  type: [CallNode]
  {
    start_node: [ViewCart-Prefix]
    call_mode_permissions: []
    parameter_bindings [
    ]
    return_value_bindings [
    ]
  }
  successors [
    {
      name: [next]
      transaction_mode: [null]
      next_node_type: [PipeletNode]
    }
  ]
}
{
  type: [PipeletNode]
  pipelet: [enfinity:/sld_ch_b2c_base/pipelets/GetBasketProductLineItemBO.xml]
  configuration_values [
  ]
  parameter_bindings [
    {
      name: [BasketBO]
      constant: [null]
      is_nullBinding: [null]
      object_path: [CurrentCartBO]
    }
    {
      name: [PLIID]
      constant: [null]
      is_nullBinding: [null]
      object_path: [removeProduct]
    }
  ]
  return_value_bindings [
    {
      name: [BasketProductLineItemBO]
      constant: [null]
      is_nullBinding: [null]
      object_path: [ProductLineItem]
    }
  ]
  successors [
    {
      name: [next]
      transaction_mode: [null]
      next_node_type: [CallNode]
    }
    {
      name: [pipelet_error]
      transaction_mode: [null]
      next_node_type: [JoinNode]
    }
  ]
}
{
  type: [JoinNode]
  successors [
    {
      name: [next]
      transaction_mode: [null]
      next_node_type: [JumpNode]
    }
  ]
}
{
  type: [JumpNode]
  start_node: [ViewCart-View]
  call_mode_permissions: []
  parameter_bindings [
  ]
}
{
  type: [CallNode]
  {
    start_node: [ProcessBasket-RemoveLineItem]
    call_mode_permissions: []
    parameter_bindings [
    ]
    return_value_bindings [
    ]
  }
  successors [
    {
      name: [Error]
      transaction_mode: [null]
      next_node_type: [JoinNode]
    }
    {
      name: [next]
      transaction_mode: [null]
      next_node_type: [JumpNode]
    }
  ]
}
```

# Usage

```
Usage: icm-pipeline-ecpf-converter [-hv] [--only-common] [-o=<outputDirectory>] --system-cartridges-dir=<systemCartridgesDir> [-n=startNodeName[,startNodeName...]]... pipelineFile...                    
      pipelineFile...   An Intershop pipeline file to process.
  -h, --help            Display program's usage.
  -n, --start-nodes=startNodeName[,startNodeName...]
                        List of start nodes to generate the ECPF file for. When used without the --only-common option, an ECPF file will be generated only for the start nodes included in this list.     
                          When used with the --only-common option, an ECPF file will be generated only for the common start nodes which are also included in this list. When both this and --only-common  
                          options are omitted, an ECPF file will be generated for each start node in each pipeline file given.
  -o, --output-dir=<outputDirectory>
                        Output directory for ECPF files. Default is the current directory. Must exist before invoking the program.
      --only-common     If specified without the --start-nodes option, ECPF files will be generated only for start nodes in common between all the given pipeline files. If specified together with the   
                          --start-nodes option, ECPF files will be generated only for start nodes in common between all the given pipeline files which are also included in the list given to the
                          --start-nodes option.
      --system-cartridges-dir=<systemCartridgesDir>
                        The path to the system cartridges directory. Required to properly handle the type of pipeline nodes.
  -v, --verbose         Make the program more verbose.
```

## Example for `--start-nodes` and `--only-common` options

To better clarify these 2 options, an example is provided here.

In this example, 3 pipelines are given to the program:
1. start nodes of pipeline 1: `Dispatch`, `View`, `AddProduct`, `EditProduct`;
2. start nodes of pipeline 2: `Dispatch`, `EmptyCart`, `AddProduct`;
3. start nodes of pipeline 3: `AddProduct`

| `--start-nodes`       | `--only-common` | result                                                                                                                                                                                                                                                                                                                                                                        |
|-----------------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| not provided          | not provided    | 7 ECPF files generated (one for each start node)                                                                                                                                                                                                                                                                                                                              |
| `Dispatch,AddProduct` | not provided    | 5 ECPF files generated: <ol><li>one for the `Dispatch` start node in the first pipeline;</li><li>one for the `AddProduct` start node in the first pipeline;</li><li>one for the `Dispatch` start node in the second pipeline;</li><li>one for the `AddProduct` start node in the second pipeline;</li><li>one for the `AddProduct` start node in the third pipeline</li></ol> |
| not provided          | provided        | 3 ECPF files generated, one for each occurrence of the `AddProduct` start node (the only common start node between the 3 pipelines)                                                                                                                                                                                                                                           |
| `Dispatch`            | provided        | no ECPF file generated, because not all the pipelines have the `Dispatch` start node                                                                                                                                                                                                                                                                                          |

# Installation

Requirements:
- JDK >= 17

No installation needed. Download the executable JAR file from the Releases page and execute it using `java -jar`.

# Compatible ICM versions

This program has been tested only for ICM 7.10 pipeline files. For other versions, changes in the XSD files may be needed.