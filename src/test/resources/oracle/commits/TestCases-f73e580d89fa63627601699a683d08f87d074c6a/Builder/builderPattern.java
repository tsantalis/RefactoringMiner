public class builderPattern
{
    public static void main(String[] args) {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .withLivenessProbe(getLivenessProbe())
                .withReadinessProbe(getReadinessProbe())
                .endContainer()
                .withVolumes(getVolumes())
                .endSpec()
                .endTemplate()
                .endSpec()
                .endReplicationControllerItem();
    }
}