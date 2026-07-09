CREATE INDEX idx_sale_date
    ON sale(sale_date);

CREATE INDEX idx_customer
    ON sale(customer_id);

CREATE INDEX idx_execution_status
    ON import_execution(status);

CREATE INDEX idx_error_execution
    ON import_error(execution_id);