alias runhist='history | grep run_experiments.py'

ROOT_DIR=`pwd`
#ROOT_DIR=`dirname $(readlink -f $0)`
#
# Better way:
# (from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in)
# SOURCE="${BASH_SOURCE[0]}"
# while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
#   DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
#   SOURCE="$(readlink "$SOURCE")"
#   [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
# done
# DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"


echo "Setting CLASSPATH from maven."
export CLASSPATH=$ROOT_DIR/target/classes
export CLASSPATH=$CLASSPATH:`mvn exec:exec -q -Dexec.executable="echo" -Dexec.args="%classpath"`
# The old way of setting the class path is below, we simply used the
# output classes and the lib directory. Yet, we now have maven dependencies
# that are not included in the lib directory.


echo "Setting PYTHONPATH."
export PYTHONPATH=$ROOT_DIR/scripts

echo "Adding to PATH."
export PATH=$ROOT_DIR/scripts/experiments:$PATH
