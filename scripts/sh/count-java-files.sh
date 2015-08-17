for dir in */
do 
  echo $(cat ${dir}.git/config | grep -o "https://github.com/\S*") $(find ${dir} -name "*.java" | wc -l)
done


# Projects with name duplicates
# update projectgit set java_files = 187 where cloneUrl = 'https://github.com/owncloud/android.git';
# update projectgit set java_files = 1498 where cloneUrl = 'https://github.com/druid-io/druid.git';
# update projectgit set java_files = 545 where cloneUrl = 'https://github.com/cloudera/flume.git';
# update projectgit set java_files = 906 where cloneUrl = 'https://github.com/apache/storm.git';
