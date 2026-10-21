#!/bin/bash
# Executado pelo LocalStack quando o serviço estiver pronto (hook ready.d).
set -e
awslocal s3 mb s3://payment-receipts || true
