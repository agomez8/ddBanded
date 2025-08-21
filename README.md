
# ConicLTS



The software and data in this repository are a snapshot of the software and data
that were used in the research reported on in the paper 
[Real-time solution of quadratic optimization problems with banded matrices and indicator variables](https://arxiv.org/abs/2405.03051) by Andres Gomez, Shaoning Han and Leonardo Lozano. 


**Important: This code is being developed on an on-going basis at 
https://github.com/agomez8/ddBanded. Please go there if you would like to
get a more recent version or would like support**

## Cite

<!--To cite the contents of this repository, please cite both the paper and this repo, using their respective DOIs.


https://doi.org/10.1287/ijoc.2025.1215

https://doi.org/10.1287/ijoc.2025.1215.cd

Below is the BibTex for citing this snapshot of the repository.

```
@misc{ConicLTS,
  author =        {Andres Gomez and Jose Neto},
  publisher =     {INFORMS Journal on Computing},
  title =         {{Outlier detection in regression: Conic quadratic formulations}},
  year =          {2025},
  doi =           {10.1287/ijoc.2025.1215.cd},
  url =           {https://github.com/INFORMSJoC/2025.1215},
  note =          {Available for download at https://github.com/INFORMSJoC/2025.1215},
}  

```
-->

Below is the BibTex for citing this snapshot of the repository.

```
@misc{ddBanded,
  author =        {Andres Gomez, Shaoning Han and Leonardo Lozano},
  title =         {{Real-time solution of quadratic optimization problems with banded matrices and indicator variables}},
  year =          {2025},          
  url =           {https://github.com/agomez8/ddBanded},
  note =          {Available for download at https://github.com/agomez8/ddBanded},
}  

```


## Description

The goal of this software is to demonstrate the use of decision diagrams to solve mixed-integer quadratic optimization problems with banded matrices and indicator variables.

The methods are implemented in Java and rely on the commercial solver Mosek. Executing the code requires a license for this solver.

## Executing the code

As a java code, the source code is precompiled and can be executed directly via file ./dist/MINLP.jar. Ensure to install Mosek and replace file mosek.jar in ./dist/lib with those obtaining from installing the software.

The code can be executed from the console. Files "runDD.bat" and "runDDOnline.bat" contain examples of how to execute the code in offline and online settings. 

An example command to execute the code to tackle an offline problem is
```
java  -cp ./dist/MINLPDD.jar minlpdd_ConsecutiveOnes.MINLPDD ./data/daily_data_1990.csv 25 0.001 0.25 2 0 101 0
```
where: "java  -cp ./dist/MINLP.jar" points to the direction of the executable jar file, and "minlpdd_ConsecutiveOnes.MINLPDD" is the class used to run offline instances. The rest of parameters are as follows:
* First parameter (./data/daily_data_1990.csv) is a path to the dataset to use
* Second parameter (25) is the number of time periods to use
* Third parameter (0.001) is the weight of the L0 parameter (objective cost for the discrete variables)
* Fourth parameter (0.25) is the weight of the smooth regularization term
* Fifth parameter (2) is the bandwidth of the smooth regularization term
* Sixth parameter (0) is the minimum number of consecutive ones (by default, 0 means no constraint)
* Seventh parameter (101) is the seed
* Eighth parameter (0) is the method to be used. Method: 0: Mosek based on perspective reformulation. 1: Decision diagram.

An example command to execute the code to simulate solution in an online setting is
```
java  -cp ./dist/MINLPDD.jar minlpdd_ConsecutiveOnes.MINLPDDOnline ./data/daily_data_1990.csv 7022 0.001 0.25 2 0 200 101 1
```
where: "java  -cp ./dist/MINLPDD.jar minlpdd_ConsecutiveOnes.MINLPDDOnline" points to the direction of the executable jar file, and "minlpdd_ConsecutiveOnes.MINLPDDOnline" is the class used to run online instances. The rest of parameters are as follows:
* First parameter (./data/daily_data_1990.csv) is a path to the dataset to use
* Second parameter (7022) is the number of total time periods
* Third parameter (0.001) is the weight of the L0 parameter (objective cost for the discrete variables)
* Fourth parameter (0.25) is the weight of the smooth regularization term
* Fifth parameter (2) is the bandwidth of the smooth regularization term
* Sixth parameter (0) is the minimum number of consecutive ones (by default, 0 means no constraint)
* Seventh parameter (200) is the horizon (i.e., problems are solved at each step using this number of previous datapoints)
* Seventh parameter (101) is the seed
* Eighth parameter (1) is the method to be used. By default it is one, referring to decision diagrams.

## Output

After solving an instance, the results are recorded in "./results/resultsOffline.csv" (for offline instances) and "./results/resultsOnline.csv" (for online instances). Each instance solved is added as a new row to these files. 

For offline problems and method =0 (Mosek), each row is organized as follows:
* Columns 1-8:  are the parameters used to generate the instance
* Column 9: 0
* Column 10: 0
* Column 11: 0
* Column 12: 0
* Column 13: Time spent in branch-and-bound
* Column 14: 0
* Column 15: 0
* Column 16: Objective value as reported by Mosek
* Column 17: Objective value computed by retrieving the optimal discrete variables from Mosek, and computing the objective in closed form
* Column 18: Number of branch and bound nodes
* Column 19: Relative gap reported by Mosek after branch-and-bound
* Column 20: 0
* Column 21: 0
* Column 22: 0
* Column 23: 0

For online problems, each row is organized as follows:
* Columns 1-8:  are the parameters used to generate the instance
* Column 9: Number of nodes in the decision diagram
* Column 10: Number of arcs in the decision diagram
* Column 11: Time to construct the decision diagram
* Column 12: Total time spent solving shortest paths in the decision diagram
* Column 13: 0



## Replicating

To replicate the results in offline setting, reported in Table 1, Table 2 and Figure 5, Figure 6 and Figure 7 of the paper, use file runDD.bat (on a Windows machine).

To replicate the results in online setting, reported in Figure 4 and Figure 1 of the paper, use file runDDOnline.bat (on a Windows machine).

## Source code
The source code can be found in the src folder.


