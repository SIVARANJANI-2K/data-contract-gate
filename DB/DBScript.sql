CREATE DATABASE data_contract_db;

USE data_contract_db;

SELECT DATABASE();

CREATE TABLE datasets (
    dataset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_name VARCHAR(100) NOT NULL UNIQUE,
    producer_name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contract_versions (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    schema_definition JSON NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_contract_dataset
        FOREIGN KEY (dataset_id)
        REFERENCES datasets(dataset_id),

    CONSTRAINT uq_dataset_version
        UNIQUE (dataset_id, version)
);

CREATE TABLE contract_fields (
    field_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    is_nullable BOOLEAN DEFAULT FALSE,
    is_required BOOLEAN DEFAULT TRUE,

    CONSTRAINT fk_field_contract
        FOREIGN KEY (contract_id)
        REFERENCES contract_versions(contract_id),

    CONSTRAINT uq_contract_field
        UNIQUE (contract_id, field_name)
);

CREATE TABLE consumers (
    consumer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consumer_name VARCHAR(150) NOT NULL UNIQUE,
    consumer_type VARCHAR(50) NOT NULL,
    owner VARCHAR(150),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consumer_dependencies (
    dependency_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consumer_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    usage_type VARCHAR(100),

    CONSTRAINT fk_dependency_consumer
        FOREIGN KEY (consumer_id)
        REFERENCES consumers(consumer_id),

    CONSTRAINT fk_dependency_dataset
        FOREIGN KEY (dataset_id)
        REFERENCES datasets(dataset_id),

    CONSTRAINT uq_consumer_dependency
        UNIQUE (consumer_id, dataset_id, field_name)
);

CREATE TABLE validation_runs (
    validation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    contract_id BIGINT,
    base_contract_id BIGINT,
    validation_type VARCHAR(40) NOT NULL,
    source_file VARCHAR(500),
    result VARCHAR(20) NOT NULL,
    error_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    CONSTRAINT fk_validation_dataset
        FOREIGN KEY (dataset_id)
        REFERENCES datasets(dataset_id),

    CONSTRAINT fk_validation_contract
        FOREIGN KEY (contract_id)
        REFERENCES contract_versions(contract_id),

    CONSTRAINT fk_validation_base_contract
        FOREIGN KEY (base_contract_id)
        REFERENCES contract_versions(contract_id)
);

CREATE TABLE drift_details (
    drift_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    validation_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    severity VARCHAR(20),
    reason TEXT,

    CONSTRAINT fk_drift_validation
        FOREIGN KEY (validation_id)
        REFERENCES validation_runs(validation_id)
);

INSERT INTO datasets
(dataset_name, producer_name, description)
VALUES
('orders', 'Ecommerce Application', 'Customer order information'),
('customers', 'Customer Application', 'Customer profile information'),
('payments', 'Payment Application', 'Payment transaction information');

INSERT INTO consumers
(consumer_name, consumer_type, owner)
VALUES
('Snowflake Orders', 'DATA_WAREHOUSE', 'Analytics Team'),
('Power BI Sales Dashboard', 'REPORTING', 'BI Team'),
('Reporting API', 'APPLICATION', 'Backend Team');

INSERT INTO contract_versions
(dataset_id, version, schema_definition, status)
VALUES
(
    1,
    '1.0.0',
    '{
        "fields": {
            "order_id": {
                "type": "integer",
                "nullable": false
            },
            "customer_name": {
                "type": "string",
                "nullable": false
            },
            "amount": {
                "type": "decimal",
                "nullable": false
            }
        }
    }',
    'APPROVED'
);

INSERT INTO contract_fields
(contract_id, field_name, data_type, is_nullable, is_required)
VALUES
(1, 'order_id', 'integer', FALSE, TRUE),
(1, 'customer_name', 'string', FALSE, TRUE),
(1, 'amount', 'decimal', FALSE, TRUE);



INSERT INTO consumer_dependencies
(consumer_id, dataset_id, field_name, usage_type)
VALUES
(1, 1, 'amount', 'SUM'),
(2, 1, 'amount', 'SUM'),
(2, 1, 'customer_name', 'GROUP_BY'),
(3, 1, 'order_id', 'LOOKUP');

SELECT * FROM datasets;

SELECT * FROM contract_versions;

SELECT * FROM contract_fields;

SELECT * FROM consumers;

SELECT * FROM consumer_dependencies;

SELECT
    c.consumer_name,
    cd.field_name,
    cd.usage_type
FROM consumer_dependencies cd
JOIN consumers c
    ON cd.consumer_id = c.consumer_id
JOIN datasets d
    ON cd.dataset_id = d.dataset_id
WHERE d.dataset_name = 'orders'
AND cd.field_name = 'amount';

UPDATE contract_versions
SET schema_definition = JSON_SET(
    schema_definition,
    '$.fields.order_id.required', TRUE,
    '$.fields.customer_name.required', TRUE,
    '$.fields.amount.required', TRUE
)
WHERE contract_id = 1;

delete from contract_fields where contract_id = 2;
delete from contract_versions where  contract_id =2;

select * from validation_runs;

select * from drift_details;
