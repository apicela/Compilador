package org.example;

public class FinalToken {
        private String type; //constante que representa o token
        private String identifier;
        private String value;

        public FinalToken(String type, String identifier){
            this.type = type;
            this.identifier = identifier;
        }
        public FinalToken(){}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "FinalToken{" +
                "type='" + type + '\'' +
                ", identifier='" + identifier + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
