package io.mapper;

import io.model.apiweather.LocationResponse;
import io.model.dto.LocationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LocationApiMapper {

    LocationDTO toDto(LocationResponse response);

    List<LocationDTO> toDto(List<LocationResponse> responses);

}
