from datetime import datetime

from pydantic import BaseModel, ConfigDict


class Chat(BaseModel):
    chat_id: int
    chat_name: str | None = None
    chat_avatar_file_id: int | None = None
    is_group: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
