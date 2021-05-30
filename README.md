# GLO (a game library organizer)

## Build from source

```
git clone git@github.com:softwareloop/glo.git
cd glo
mvn clean package
```

## Set up

```
export DATDIR=/path/to/your/dat/directory
PATH=$PATH:/path/to/glo/bin
```

## Usage

```
usage: glo.sh [OPTION]... [ROMDIR]...
 -datdir <DIR>   set the dat directory (overrides $DATDIR)
 -help           print this usage information
 -rename         rename roms to official name
 -unzip          extract roms from zip files
 -verbose        be extra verbose
```

## Example

Get some help:

```
glo.sh -help
```

Scan a ROM dir:

```
glo.sh path/to/your/roms
```

Scan the current directory:

```
glo.sh .
```

Rename your ROM files to their official names according to the DATs:

```
glo.sh -rename path/to/your/roms
```

Extract single rom files from zip files

```
glo.sh -unzip path/to/your/roms
```
