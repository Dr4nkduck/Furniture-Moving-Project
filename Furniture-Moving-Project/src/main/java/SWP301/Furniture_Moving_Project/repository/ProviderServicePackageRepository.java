// repository/ProviderServicePackageRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderServicePackageRepository extends JpaRepository<ProviderServicePackage, Integer> {

    Optional<ProviderServicePackage> findByProviderIdAndPackageId(Integer providerId, Integer packageId);

    @Query("""
       select psp from ProviderServicePackage psp
       where psp.providerId = :pid
    """)
    List<ProviderServicePackage> findAllByProviderId(@Param("pid") Integer providerId);

    @Modifying
    @Query("""
       update ProviderServicePackage p set p.perKm = :perKm, p.packageNameSnapshot=:snap
       where p.providerId=:pid and p.packageId=:pkgId
    """)
    int updateSnapshot(@Param("pid") Integer providerId,
                       @Param("pkgId") Integer packageId,
                       @Param("perKm") Double perKm,
                       @Param("snap") String packageNameSnapshot);


   @Modifying
@Query("""
       delete from ProviderServicePackage p
       where p.providerId = :pid and p.packageId = :pkgId
       """)
int deleteByProviderIdAndPackageId(@Param("pid") Integer providerId,
                                   @Param("pkgId") Integer packageId);

}
