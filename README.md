# (Refactor in Progress) Systematic Identification of Scientific Articles with Reduced user Effort

**NOTE:** Previously work were made in [mvtodescato's CAL fork](https://github.com/mvtodescato/CAL), however, since we don't intend to merge it with the original upstream, we decided to create another repository, therefore all modifications since 5 March, 2020 was made in this repository.

## Overview
In this project, we develop a novel approach, applying active learning, to reduce user effort for relevant document identification in the context of systematic review of academic papers.  

We've applied our work on medical articles, from {ADD DATASET NAME WITH REFERENCE}.

## Installation
1. Clone this repository and enter it:
`git clone https://github.com/jeanchilger/scal.git && cd SCAL`
1. Inside repository, build kissdb indexer and other necessary binaries:
`make`

You are ready to go

## Usage
The bash file named `main` is the entry point for the system. Type `./main <options>` for using it. Check the available options below.

```bash
-s <samples>, --samples=<samples>
      Set <samples> as the quantity of executions that will occurs.
      After, the mean and standard deviation over the samples are taken.
      The standard value is 1.

-c <corpus>, --corpus=<corpus>
      Specifies the name of the corpus to be used.

-t <topic-list>, --topics=<topic-list>
      Specifies which topics will be computed by the method.
      <topic-list> must be a space separated string, containing
      one or more topics.

-v, --verbose
      If specified, verbose messages will be shown during execution.

-o, --off-colors
      Turns off colors of terminal outputs.

-h, --help
      Show a message like this.
```

## Team
### Coordinating professor
- Guilherme Dal Bianco
  - [GitHub](https://github.com/dbguilherme)
  - [Lattes (pt-br Curriculum)]( http://lattes.cnpq.br/5152594034228273)

### Academics participating
- Emili Willinghoefer (early work)
  - [GitHub](https://github.com/Emiliwillinghoefer)
- Jean Carlo Hilger
  - [GitHub](https://github.com/jeanchilger)
- Matheus Vinícius Todescato
  - [GitHub](https://github.com/mvtodescato)

## Credits
This is a work intended to improve the SCAL (Scalable Continuous Active Learning) protocol for TAR (Technology Assisted Review) processes, developed by [ADD REFERENCE]. The base code used as basis is from [HTAustin](https://github.com/HTAustin)'s [CAL repository](https://github.com/HTAustin/CAL).
