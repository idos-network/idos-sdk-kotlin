#!/bin/bash

# Script to generate Config.swift from .env file
# This keeps sensitive values out of the repository while making them available at build time

set -e

# Paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
ENV_FILE="$PROJECT_ROOT/.env"
OUTPUT_FILE="$SCRIPT_DIR/iosApp/Core/Config.swift"

# Ensure output directory exists
mkdir -p "$(dirname "$OUTPUT_FILE")"

# Default values
MNEMONIC_WORDS=""
PASSWORD=""

# Read .env file if it exists
if [ -f "$ENV_FILE" ]; then
    # Parse values from .env
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        [[ $key =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue

        # Remove leading/trailing whitespace
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)

        if [ "$key" = "MNEMONIC_WORDS" ]; then
            MNEMONIC_WORDS="$value"
        elif [ "$key" = "PASSWORD" ]; then
            PASSWORD="$value"
        fi
    done < "$ENV_FILE"
else
    echo "Warning: .env file not found at $ENV_FILE"
    echo "Config.swift will be generated with empty values"
fi

# Generate Config.swift
cat > "$OUTPUT_FILE" << EOF
//
// Config.swift
// iosApp
//
// Auto-generated from .env file at build time
// DO NOT EDIT MANUALLY - This file is regenerated on every build
// DO NOT COMMIT - This file is excluded in .gitignore
//

import Foundation

enum Config {
    #if DEBUG
    /// Development mnemonic from .env file (DEBUG builds only)
    static let developmentMnemonic = "$MNEMONIC_WORDS"
    /// Development password from .env file (DEBUG builds only)
    static let developmentPassword = "$PASSWORD"
    #else
    /// Development mnemonic (empty in Release builds)
    static let developmentMnemonic = ""
    /// Development password (empty in Release builds)
    static let developmentPassword = ""
    #endif
}
EOF

echo "✅ Config.swift generated successfully"
