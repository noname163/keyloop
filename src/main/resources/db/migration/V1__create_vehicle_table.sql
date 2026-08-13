CREATE TABLE vehicle (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    vin VARCHAR(17) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vehicle_tenant_vin UNIQUE (tenant_id, vin)
);

CREATE INDEX idx_vehicle_tenant_id ON vehicle (tenant_id);
