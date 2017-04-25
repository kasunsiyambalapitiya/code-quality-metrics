# Welcome to Code Quality Metrics

At WSO2 when a customer report a bug on a product, often we have to issue a patch to solve the problem. As part of providing this patch, we also record the commit hashes which fixed the issue in WSO2 Patch Management System. What this program does is, it takes those commit hashes and identify the initial lines which caused the bug to arose, who authored them, reviewed them and approved them. The reason for doing this is to see whether there are any patterns on the bugs introduced to the code. If so, WSO2 can educate the people on relevant patterns and prevent the bugs in future releases. 

## Architecture
Code Quality Metrics application make API calls to WSO2 PMT REST API, Github REST API v3 and Github Graphql API in it’s execution. PMT REST API calls are made to obtain the relevant commit hashes reside in the given patch, Github REST API calls are made to identify the relevant WSO2 repositories containing the obtained commits ( obtained from WSO2 PMT REST API for the given patch), to identify whether those commits are formed from a octopus merge commit, to obtain commit history of a given file, to identify relevant pull requests containing the obtained author commits and to identify relevant reviewers of those pull requests if any.

Since there is no Github REST API yet deployed for obtaining blame details of files, Github Graphql API was used to get the blame details of modified files from above obtained commits, even though the GraphQL API is still in early access mode.

[Github Java API](https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core) was used for obtaining the modified files and their relevant patch strings for the commits resides on the given patch.

<p align="center">
<img src="https://i.imgur.com/Ms1Kv4b.png" alt="Architectural Diagram"/>
</p>


## Installation
### Prerequisites
* [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) installed.
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) installed.
* [Apache Maven](https://maven.apache.org/download.cgi) installed.


## Running Samples
It is required to provide the relevant WSO2 patch name as a command line argument to obtain relevant authors and reviewers of code lines been modified from the given patch.

After cloning or downloading the repository perform one of the following,
 1. Navigate to projects `pom.xml` location through terminal and run `mvn package` , once `BUILD SUCCESS` appears navigate to project's `target` directory and run `java -jar code-quality-metrics-1.0.0-SNAPSHOT.jar <patchId>` (replace `<patchId>` with the specified patch)
 2. Run `Eclipse IDE` or `IntelliJ IDEA` and open project’s `pom.xml` then run `Application.main` with providing relevant patch id as a command line argument (Import all the dependencies if required).


Run following samples and check with outputs,

1. WSO2-CARBON-PATCH-4.4.0-0680   
    Authors : [lalaji(), Chamila(Chamila Adhikarinayake), Lakmali(Lakmali Erandi), Amila De Silva()]  
    Approved Users: []  
    Commented Users: []  
    
    Note : Since this is an old patch reviewers of bug lines are not present as github protected branch feature was used by WSO2 from 2017 onwards.
2. WSO2-CARBON-PATCH-4.4.0-1134  
    Authors: []  
    Approved Users: []  
    Commented Users: []  

    Note : Since this patch only introduce new files to the repository, no authors of bug lines are shown.

3. WSO2-CARBON-PATCH-4.4.0-1102  
    Authors: [nuwandiw(), Pasindu Nivanthaka Tennage(Pasindu Nivanthaka Tennage), godwin(Godwin Amila Shrimal)]  
    Approved Users: [darshanasbg(Darshana Gunawardana)]  
    Commented Users: []  
    
    Note: Since this patch has modified lines added after enabling protected branch feature on master branches, the application provides the approved user of the bug lines as “darshanasbg” and there are no commented users on those lines.
