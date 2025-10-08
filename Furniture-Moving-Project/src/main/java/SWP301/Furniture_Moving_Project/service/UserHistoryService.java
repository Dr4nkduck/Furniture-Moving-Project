package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.DTO.UserHistoryDto;
import SWP301.Furniture_Moving_Project.model.CustomRevisionEntity;
import SWP301.Furniture_Moving_Project.model.User;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class UserHistoryService {

    private final EntityManager entityManager;

    public UserHistoryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<UserHistoryDto> getUserHistory(Long userId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(User.class, false, true)
                .add(AuditEntity.id().eq(userId))
                .getResultList();

        List<UserHistoryDto> history = new ArrayList<>();

        for (Object[] revData : revisions) {
            User userAtRevision = (User) revData[0];
            CustomRevisionEntity revisionEntity = (CustomRevisionEntity) revData[1];
            RevisionType revisionType = (RevisionType) revData[2];

            history.add(new UserHistoryDto(
                    (long) revisionEntity.getId(),         // revisionId
                    revisionEntity.getModifiedBy(),             // who modified
                    revisionEntity.getRevisionDate(),           // when
                    revisionType.name(),                        // action: INSERT/UPDATE/DELETE
                    userAtRevision                              // snapshot of entity
            ));
        }

        return history;
    }
}
