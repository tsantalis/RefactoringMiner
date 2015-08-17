for dir in */
do 
  echo $(cat ${dir}.git/config | grep -o "https://github.com/\S*") $(git --git-dir=${dir}.git shortlog -s --no-merges --all | wc -l)
done
