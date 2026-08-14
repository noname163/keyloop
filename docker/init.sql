CREATE TABLE IF NOT EXISTS vehicle (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    vin VARCHAR(17) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_vehicle_tenant_vin UNIQUE (tenant_id, vin)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_vin
    ON vehicle(vin);

CREATE INDEX IF NOT EXISTS idx_vehicle_tenant_id
    ON vehicle(tenant_id);

INSERT INTO vehicle (
    id,
    tenant_id,
    vin,
    brand,
    model,
    registration_number,
    created_at,
    updated_at
)
VALUES
(
    'a0000001-0000-0000-0000-000000000001',
    'TENANT-001',
    '1HGCM82633A123456',
    'Honda',
    'Accord',
    '51H-123.45',
    '2026-01-05T08:00:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000002-0000-0000-0000-000000000002',
    'TENANT-001',
    'JH4KA9650MC012345',
    'Acura',
    'Legend',
    '51H-234.56',
    '2026-02-10T07:30:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000003-0000-0000-0000-000000000003',
    'TENANT-001',
    '1FTFW1ET4EFA12345',
    'Ford',
    'F-150',
    '51H-345.67',
    '2026-03-15T08:30:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000004-0000-0000-0000-000000000004',
    'TENANT-002',
    'KMHDU46D08U123456',
    'Hyundai',
    'Elantra',
    '51K-456.78',
    '2026-04-02T08:00:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000005-0000-0000-0000-000000000005',
    'TENANT-002',
    'WBA3A5C50DF123456',
    'BMW',
    '3 Series',
    '51K-567.89',
    '2026-05-20T07:00:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000006-0000-0000-0000-000000000006',
    'TENANT-002',
    '5YJSA1E26HF123456',
    'Tesla',
    'Model S',
    '51K-678.90',
    '2026-06-08T11:00:00Z',
    '2026-08-12T12:00:00Z'
),
(
    'a0000007-0000-0000-0000-000000000007',
    'TENANT-003',
    'JTDBR32E720123456',
    'Toyota',
    'Corolla',
    '51L-789.01',
    '2026-07-12T08:00:00Z',
    '2026-08-12T12:00:00Z'
)
ON CONFLICT DO NOTHING;
