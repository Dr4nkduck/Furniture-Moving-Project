// repository/ProviderPackageFurniturePriceRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ProviderPackageFurniturePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderPackageFurniturePriceRepository extends JpaRepository<ProviderPackageFurniturePrice, Integer> {

    @Query("""
              select x from ProviderPackageFurniturePrice x
              where x.providerId=:pid and x.packageId=:pkgId
              order by x.furnitureTypeId asc
            """)
    List<ProviderPackageFurniturePrice> findByProviderAndPackage(@Param("pid") Integer providerId,
                                                                 @Param("pkgId") Integer packageId);

    @Modifying
    @Query("delete from ProviderPackageFurniturePrice x where x.providerId=:pid and x.packageId=:pkgId")
    int deleteAllByProviderAndPackage(@Param("pid") Integer providerId, @Param("pkgId") Integer packageId);

    @Modifying
    @Query("""
              delete from ProviderPackageFurniturePrice x
              where x.providerId=:pid and x.packageId=:pkgId and x.furnitureTypeId=:fid
            """)
    int deleteOne(@Param("pid") Integer providerId, @Param("pkgId") Integer packageId, @Param("fid") Integer furnitureTypeId);

    @Query("""
              select x from ProviderPackageFurniturePrice x
              where x.providerId=:pid and x.packageId=:pkgId and x.furnitureTypeId=:fid
            """)
    ProviderPackageFurniturePrice findOne(@Param("pid") Integer providerId, @Param("pkgId") Integer packageId, @Param("fid") Integer furnitureTypeId);
}
