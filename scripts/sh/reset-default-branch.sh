for dir in */
do 
  defaultBranch=$(cat ${dir}.git/config | grep -oP 'branch "\K[^"]+')
  cd ${dir}
  git reset --hard origin/${defaultBranch}
  cd ..
done
