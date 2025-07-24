from pydantic import BaseModel, PositiveInt, ConfigDict


class UserCreate(BaseModel):
    username: str
    password: str

class User(BaseModel):
    user_id: PositiveInt
    username: str
    is_disabled: bool

    model_config = ConfigDict(extra="ignore", from_attributes=True)

class UserInDb(User):
    hashed_password: str