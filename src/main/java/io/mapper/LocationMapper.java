package io.mapper;

import io.model.dto.LocationDTO;
import io.model.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LocationMapper {

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lon", target = "longitude")
    Location toEntity(LocationDTO locationDTO);

}